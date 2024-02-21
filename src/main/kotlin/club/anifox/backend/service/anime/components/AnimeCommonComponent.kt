package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.enums.anime.filter.AnimeEpisodeFilter
import club.anifox.backend.domain.exception.common.NoContentException
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.detail.toAnimeDetail
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeLight
import club.anifox.backend.domain.mappers.anime.toAnimeMedia
import club.anifox.backend.domain.mappers.anime.toGenre
import club.anifox.backend.domain.mappers.anime.toStudio
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeMedia
import club.anifox.backend.domain.model.anime.AnimeRelation
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.jpa.entity.anime.AnimeMediaTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.util.AnimeUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeCommonComponent {

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    private lateinit var animeGenreRepository: AnimeGenreRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    fun getAnimeByUrl(url: String): AnimeDetail {
        val anime = animeUtils.checkAnime(url)
        return anime.toAnimeDetail()
    }

    fun getAnimeSimilar(url: String): List<AnimeLight> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("similarAnime", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        } else {
            val similarCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
            val similarRoot = similarCriteriaQuery.from(AnimeTable::class.java)
            similarCriteriaQuery.select(similarRoot)
                .where(similarRoot.get<Int>("shikimoriId").`in`(anime[0].similarAnime))

            val similarEntityList = entityManager.createQuery(similarCriteriaQuery).resultList as List<AnimeTable>

            val similarEntityMap: Map<Int, AnimeTable> = similarEntityList.associateBy { it.shikimoriId }

            val similarAnimeList: List<AnimeTable> = anime[0].similarAnime.mapNotNull { similarAnimeId ->
                similarEntityMap[similarAnimeId]
            }

            if (similarAnimeList.isNotEmpty()) {
                return similarAnimeList.map { it.toAnimeLight() }
            }

            throw NoContentException("Anime has no similar anime")
        }
    }

    fun getAnimeRelated(url: String): List<AnimeRelationLight> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("related", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime with url = $url not found")
        } else {
            val relatedAnimeList: List<AnimeRelationLight> = anime[0].related.mapNotNull { related ->
                val relatedCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
                val relatedRoot = relatedCriteriaQuery.from(AnimeTable::class.java)
                relatedCriteriaQuery.select(relatedRoot)
                    .where(criteriaBuilder.equal(relatedRoot.get<Int>("shikimoriId"), related.shikimoriId))

                val relatedAnimeList: List<AnimeTable> = entityManager.createQuery(relatedCriteriaQuery).resultList

                if (relatedAnimeList.isNotEmpty()) {
                    val relatedAnime: AnimeTable = relatedAnimeList.first()
                    val animeLight = relatedAnime.toAnimeLight()
                    AnimeRelationLight(animeLight, AnimeRelation(type = related.type, typeEn = related.typeEn))
                } else {
                    null
                }
            }

            if (relatedAnimeList.isNotEmpty()) {
                return relatedAnimeList
            }

            throw NoContentException("Anime has no related anime")
        }
    }

    fun getAnimeScreenshots(url: String): List<String> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("screenshots", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager
            .createQuery(criteriaQuery)
            .resultList
            .firstOrNull() ?: throw NotFoundException("Anime with url = $url not found")

        if (anime.screenshots.isNotEmpty()) {
            return anime.screenshots
        }

        throw NoContentException("Anime has no screenshots")
    }

    fun getAnimeMedia(url: String): List<AnimeMedia> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, AnimeMediaTable>("media", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager
            .createQuery(criteriaQuery)
            .resultList
            .firstOrNull() ?: throw NotFoundException("Anime with url = $url not found")

        if (anime.media.isNotEmpty()) {
            return anime.media.map { it.toAnimeMedia() }
        }

        throw NoContentException("Anime has no media")
    }

    fun getAnimeYears(): List<String> {
        return animeRepository.findDistinctByYear()
    }

    fun getAnimeStudios(): List<AnimeStudio> {
        return animeStudiosRepository.findAll().map { it.toStudio() }
    }

    fun getAnimeGenres(): List<AnimeGenre> {
        return animeGenreRepository.findAll().map { it.toGenre() }
    }

    fun getAnimeEpisodes(url: String, page: Int, limit: Int, sort: AnimeEpisodeFilter?): List<AnimeEpisodeLight> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeTable> = criteriaBuilder.createQuery(AnimeEpisodeTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val episodesJoin = animeRoot.join<AnimeTable, AnimeEpisodeTable>("episodes", JoinType.LEFT)

        criteriaQuery.where(criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url))

        criteriaQuery.select(episodesJoin)

        when (sort) {
            AnimeEpisodeFilter.NumberAsc -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
            AnimeEpisodeFilter.NumberDesc -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
            else -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
        }

        val query = entityManager.createQuery(criteriaQuery)

        val firstResult = (page - 1) * limit
        query.firstResult = if (firstResult >= 0) firstResult else 0
        query.maxResults = limit
        val a = query.resultList
        return query.resultList.map { it.toAnimeEpisodeLight() }
    }
}
