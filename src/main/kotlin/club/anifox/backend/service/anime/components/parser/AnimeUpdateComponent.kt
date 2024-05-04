package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class AnimeUpdateComponent {

    @Autowired
    private lateinit var animeErrorParserRepository: AnimeErrorParserRepository

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var episodesComponent: EpisodesComponent

    @Autowired
    private lateinit var shikimoriComponent: AnimeShikimoriComponent

    fun update() {
        val criteriaBuilder = entityManager.criteriaBuilder

        val currentYear = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().year
        val criteriaQueryShikimori: CriteriaQuery<Int> = criteriaBuilder.createQuery(Int::class.java)
        val shikimoriRoot = criteriaQueryShikimori.from(AnimeTable::class.java)
        criteriaQueryShikimori
            .select(shikimoriRoot.get("shikimoriId"))
            .where(criteriaBuilder.equal(shikimoriRoot.get<String>("status"), AnimeStatus.Ongoing))
            .where(criteriaBuilder.between(shikimoriRoot.get("year"), currentYear - 1, currentYear))

        val query = entityManager.createQuery(criteriaQueryShikimori)
        val shikimoriIds = query.resultList

        shikimoriIds.forEach Loop@{ shikimoriId ->
            try {
                val criteriaQueryAnime: CriteriaQuery<AnimeTable> = criteriaBuilder.createQuery(AnimeTable::class.java)
                val rootAnime = criteriaQueryAnime.from(AnimeTable::class.java)

                rootAnime.fetch<AnimeEpisodeTable, Any>("episodes", JoinType.LEFT)
                rootAnime.fetch<AnimeEpisodeTranslationCountTable, Any>("translationsCountEpisodes", JoinType.LEFT)
                rootAnime.fetch<AnimeIdsTable, Any>("ids", JoinType.RIGHT)
                rootAnime.fetch<AnimeTranslationTable, Any>("translations", JoinType.LEFT)

                criteriaQueryAnime.select(rootAnime)
                    .where(criteriaBuilder.equal(rootAnime.get<Int>("shikimoriId"), shikimoriId))

                val anime = entityManager.createQuery(criteriaQueryAnime).resultList[0]

                val shikimori = shikimoriComponent.checkShikimori(anime.shikimoriId)
                val episodesReady = mutableListOf<AnimeEpisodeTable>()

                episodesReady.addAll(episodesComponent.fetchEpisodes(shikimoriId = anime.shikimoriId, kitsuId = anime.ids.kitsu.toString(), type = anime.type, urlLinking = anime.url, defaultImage = anime.images.medium))

                val translationsCountReady = episodesComponent.translationsCount(episodesReady)

                val translations = translationsCountReady.map { it.translation }

                val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.of("Europe/Moscow"))

                if (shikimori != null) {
                    anime.nextEpisode = if (shikimori.nextEpisodeAt != null) {
                        LocalDateTime.parse(shikimori.nextEpisodeAt, formatterUpdated)
                    } else {
                        null
                    }
                    if (anime.description.isEmpty()) {
                        anime.description = shikimori.description.ifEmpty { anime.description }.replace(Regex("\\[\\/?[a-z]+.*?\\]"), "")
                    }
                    var countVotes = 0
                    shikimori.usersRatesStats.forEach {
                        countVotes += it.value
                    }
                    anime.shikimoriVotes = countVotes
                    anime.shikimoriRating = try {
                        shikimori.score.toDouble()
                    } catch (_: Exception) {
                        0.0
                    }
                    anime.status = when (shikimori.status) {
                        "released" -> AnimeStatus.Released
                        "ongoing" -> AnimeStatus.Ongoing
                        else -> AnimeStatus.Ongoing
                    }
                    if (anime.episodesAired < episodesReady.size) {
                        anime.updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
                    }
                    anime.episodesCount = shikimori.episodes
                    anime.episodesAired = episodesReady.size
                } else {
                    anime.episodesAired = episodesReady.size
                }

                anime.addEpisodesAll(episodesReady)
                anime.addTranslation(translations)
                anime.addTranslationCount(translationsCountReady)
                animeRepository.saveAndFlush(anime)
            } catch (e: Exception) {
                e.stackTrace.forEach {
                    println(it)
                }
                animeErrorParserRepository.save(
                    AnimeErrorParserTable(
                        message = e.message,
                        cause = "UPDATE",
                        shikimoriId = shikimoriId,
                    ),
                )
                return@Loop
            }
        }
    }
}
