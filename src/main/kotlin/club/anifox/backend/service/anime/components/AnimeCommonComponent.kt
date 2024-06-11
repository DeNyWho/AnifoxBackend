package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeEpisodeFilter
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.domain.exception.common.NoContentException
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.detail.toAnimeDetail
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeLight
import club.anifox.backend.domain.mappers.anime.toAnimeVideo
import club.anifox.backend.domain.mappers.anime.toGenre
import club.anifox.backend.domain.mappers.anime.toStudio
import club.anifox.backend.domain.model.anime.AnimeFranchise
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.jpa.entity.anime.AnimeBlockedTable
import club.anifox.backend.jpa.entity.anime.AnimeFranchiseTable
import club.anifox.backend.jpa.entity.anime.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.AnimeVideoTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.AnimeUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeCommonComponent {

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    private lateinit var animeBlockedRepository: AnimeBlockedRepository

    @Autowired
    private lateinit var animeGenreRepository: AnimeGenreRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    @Autowired
    private lateinit var imageService: ImageService

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
//            val similarCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
//            val similarRoot = similarCriteriaQuery.from(AnimeTable::class.java)
//            similarCriteriaQuery.select(similarRoot)
//                .where(similarRoot.get<Int>("shikimoriId").`in`(anime[0].similarAnime))
//
//            val similarEntityList = entityManager.createQuery(similarCriteriaQuery).resultList as List<AnimeTable>
//
//            val similarEntityMap: Map<Int, AnimeTable> = similarEntityList.associateBy { it.shikimoriId }
//
//            val similarAnimeList: List<AnimeTable> = anime[0].similarAnime.mapNotNull { similarAnimeId ->
//                similarEntityMap[similarAnimeId]
//            }
//
//            if (similarAnimeList.isNotEmpty()) {
//                return similarAnimeList.map { it.toAnimeLight() }
//            }

            throw NoContentException("There are no similar anime")
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
//            val relatedAnimeList: List<AnimeRelationLight> = anime[0].related.mapNotNull { related ->
//                val relatedCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
//                val relatedRoot = relatedCriteriaQuery.from(AnimeTable::class.java)
//                relatedCriteriaQuery.select(relatedRoot)
//                    .where(criteriaBuilder.equal(relatedRoot.get<Int>("shikimoriId"), related.shikimoriId))
//
//                val relatedAnimeList: List<AnimeTable> = entityManager.createQuery(relatedCriteriaQuery).resultList
//
//                if (relatedAnimeList.isNotEmpty()) {
//                    val relatedAnime: AnimeTable = relatedAnimeList.first()
//                    val animeLight = relatedAnime.toAnimeLight()
//                    AnimeRelationLight(animeLight, AnimeRelation(type = related.type, typeEn = related.typeEn))
//                } else {
//                    null
//                }
//            }

//            if (relatedAnimeList.isNotEmpty()) {
//                return relatedAnimeList
//            }

            throw NoContentException("There are no related anime")
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

        throw NoContentException("There are no screenshots")
    }

    fun getAnimeVideos(url: String, type: AnimeVideoType?): List<AnimeVideo> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeVideoTable> = criteriaBuilder.createQuery(AnimeVideoTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val videosJoin = animeRoot.join<AnimeTable, AnimeVideoTable>("videos", JoinType.LEFT)

        val predicates = mutableListOf(
            criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url),
        )

        if (type != null) {
            predicates.add(criteriaBuilder.equal(videosJoin.get<AnimeVideoType>("type"), type))
        }

        criteriaQuery.select(videosJoin)
        criteriaQuery.where(*predicates.toTypedArray())

        val videos = entityManager.createQuery(criteriaQuery).resultList

        if (videos.isNotEmpty()) {
            return videos.map { it.toAnimeVideo() }
        }

        throw NoContentException("There are no videos")
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
        return query.resultList.map { it.toAnimeEpisodeLight() }
    }

    @Transactional
    fun blockAnime(url: String?, shikimoriId: Int?) {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        val predicate = when {
            url?.isNotEmpty() == true -> criteriaBuilder.equal(root.get<String>("url"), url)
            shikimoriId != null -> criteriaBuilder.equal(root.get<Int>("shikimoriId"), shikimoriId)
            else -> return
        }
        criteriaQuery.where(predicate)

        val query = entityManager.createQuery(criteriaQuery)
        val anime = query.resultList

        if (anime.isNotEmpty()) {
            val animeEntity = anime[0]

            animeEntity.apply {
//                related.clear()
                episodes.clear()
                translationsCountEpisodes.clear()
                translations.clear()
                favorites.clear()
                rating.clear()
                videos.clear()
                genres.clear()
                studios.clear()
                titleEn.clear()
                titleJapan.clear()
                synonyms.clear()
                titleOther.clear()
                episodes.clear()
                similar.clear()
                screenshots.clear()
                ids = AnimeIdsTable()
                images = AnimeImagesTable()
                franchiseMultiple.clear()
            }

            entityManager.remove(animeEntity)

            if (animeEntity.url.isNotEmpty()) {
                CompressAnimeImageType.entries.forEach { imageType ->
                    imageService.deleteObjectsInFolder("images/anime/${imageType.path}/${animeEntity.url}/")
                }
            }

            val animeBlocked = AnimeBlockedTable(
                shikimoriID = animeEntity.shikimoriId,
            )

            animeBlockedRepository.saveAndFlush(animeBlocked)
        } else {
            if (shikimoriId != null) {
                val animeBlocked = AnimeBlockedTable(
                    shikimoriID = shikimoriId,
                )

                animeBlockedRepository.saveAndFlush(animeBlocked)
            }
        }
    }

    fun getAnimeFranchise(url: String, type: AnimeRelationFranchise?): List<AnimeFranchise> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeFranchiseTable> = criteriaBuilder.createQuery(AnimeFranchiseTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val franchiseJoin = animeRoot.join<AnimeTable, AnimeFranchiseTable>("franchise", JoinType.LEFT)

        val predicates = mutableListOf(
            criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url),
        )

        if (type != null) {
            predicates.add(criteriaBuilder.equal(franchiseJoin.get<AnimeRelationFranchise>("type"), type))
        }

        criteriaQuery.select(franchiseJoin)
        criteriaQuery.where(*predicates.toTypedArray())

//        val franchisesTemp = entityManager.createQuery(criteriaQuery).resultList
//
//        if (franchisesTemp.isNotEmpty()) {
//            franchisesTemp.map { franchise ->
//                val criteriaAnimeQuery: CriteriaQuery<String> = criteriaBuilder.createQuery(String::class.java)
//                val root: Root<AnimeTable> = criteriaAnimeQuery.from(AnimeTable::class.java)
//
//                criteriaAnimeQuery.select(root.get("url"))
//                criteriaAnimeQuery.where(criteriaBuilder.equal(root.get<Long>("shikimoriId"), franchise.targetId))
//
//                val animeSource = entityManager.createQuery(criteriaAnimeQuery)
//                AnimeFranchise(
//                    anime = anime,
//                    relation = franchise.relationTypeRus,
//                    franchise.sourceId
//                )
//            }
//        }

        throw NoContentException("There are no videos")
    }
}
