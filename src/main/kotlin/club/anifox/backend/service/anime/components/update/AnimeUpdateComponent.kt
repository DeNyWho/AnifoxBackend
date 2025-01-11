package club.anifox.backend.service.anime.components.update

import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeScheduleTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    fun update() {
        val criteriaBuilder = entityManager.criteriaBuilder
        val currentYear = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().year

        // Get all ongoing anime IDs in one query
        val criteriaQueryShikimori = criteriaBuilder.createQuery(Int::class.java)
        val shikimoriRoot = criteriaQueryShikimori.from(AnimeTable::class.java)
        criteriaQueryShikimori
            .select(shikimoriRoot.get("shikimoriId"))
            .where(
                criteriaBuilder.and(
                    criteriaBuilder.between(shikimoriRoot.get("year"), currentYear - 1, currentYear),
                ),
            )

        val shikimoriIds = entityManager.createQuery(criteriaQueryShikimori).resultList.shuffled()

        // Process in batches to reduce memory usage
        shikimoriIds.chunked(4).forEach { batchIds ->
            processAnimeBatch(batchIds)
        }
    }

    @Transactional
    fun processAnimeBatch(batchIds: List<Int>) {
        val criteriaBuilder = entityManager.criteriaBuilder
        // Fetch all anime for current batch in a single query with necessary joins
        val criteriaQueryAnime = criteriaBuilder.createQuery(AnimeTable::class.java)
        val rootAnime = criteriaQueryAnime.from(AnimeTable::class.java)

        // Optimize fetches by only including necessary relationships
        rootAnime.fetch<AnimeEpisodeTable, Any>("episodes", JoinType.LEFT)
        rootAnime.fetch<AnimeIdsTable, Any>("ids", JoinType.LEFT)
        rootAnime.fetch<AnimeImagesTable, Any>("images", JoinType.LEFT)
        rootAnime.fetch<AnimeTranslationTable, Any>("translations", JoinType.LEFT)
        rootAnime.fetch<AnimeEpisodeScheduleTable, Any>("schedule", JoinType.LEFT)
        rootAnime.fetch<AnimeEpisodeTranslationCountTable, Any>("translationsCountEpisodes", JoinType.LEFT)

        criteriaQueryAnime.select(rootAnime)
            .where(rootAnime.get<Int>("shikimoriId").`in`(batchIds))

        val animeList = entityManager.createQuery(criteriaQueryAnime).resultList

        // Fetch Shikimori data in parallel
        val shikimoriData = runBlocking {
            batchIds.map { shikimoriId ->
                async { shikimoriComponent.fetchAnime(shikimoriId) }
            }.awaitAll()
        }

        // Process each anime in the batch
        animeList.forEach { anime ->
            try {
                val shikimori = shikimoriData.find { it?.id == anime.shikimoriId } ?: return@forEach

                if (!anime.isLicensed) {
                    val episodesReady = runBlocking {
                        episodesComponent.fetchEpisodes(
                            shikimoriId = anime.shikimoriId,
                            kitsuId = anime.ids.kitsu.toString(),
                            type = anime.type,
                            urlLinkPath = anime.url,
                            defaultImage = anime.images.medium,
                        )
                    }

                    // Batch process translations
                    val translationsCountReady = episodesComponent.translationsCount(episodesReady)
                    val translations = translationsCountReady.map { it.translation }

                    // Update collections efficiently
                    anime.episodes.clear()
                    anime.episodes.addAll(episodesReady)
                    anime.translations.clear()
                    anime.translations.addAll(translations.toSet())
                    anime.translationsCountEpisodes.clear()
                    anime.translationsCountEpisodes.addAll(translationsCountReady)
                }

                updateAnimeFromShikimori(anime, shikimori)
                entityManager.merge(anime)
            } catch (e: Exception) {
                animeErrorParserRepository.save(
                    AnimeErrorParserTable(
                        message = e.message,
                        cause = "UPDATE",
                        shikimoriId = anime.shikimoriId,
                    ),
                )
            }
        }

        entityManager.flush()
        entityManager.clear()
    }

    private fun updateAnimeFromShikimori(anime: AnimeTable, shikimori: ShikimoriDto) {
        val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.of("Europe/Moscow"))

        anime.apply {
            nextEpisode = shikimori.nextEpisodeAt?.let {
                LocalDateTime.parse(it, formatterUpdated)
            }
            nextEpisode?.let { updateEpisodeSchedule(it) }

            if (description.isEmpty()) {
                description = shikimori.description.ifEmpty { description }
                    .replace(Regex("\\[\\/?[a-z]+.*?\\]"), "")
            }

            anime.shikimoriVotes = shikimori.usersRatesStats.sumOf { it.value }
            shikimoriRating = try {
                shikimori.score.toDouble()
            } catch (_: Exception) {
                0.0
            }

            status = when (shikimori.status) {
                "released" -> AnimeStatus.Released
                else -> AnimeStatus.Ongoing
            }

            val currentEpisodesSize = episodes.size
            if ((episodesAired ?: 0) < currentEpisodesSize) {
                updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
            }

            episodesCount = when {
                shikimori.episodes < currentEpisodesSize -> currentEpisodesSize
                shikimori.episodes == 0 && status == AnimeStatus.Ongoing -> null
                else -> maxOf(shikimori.episodes, currentEpisodesSize)
            }

            episodesAired = currentEpisodesSize
        }
    }
}
