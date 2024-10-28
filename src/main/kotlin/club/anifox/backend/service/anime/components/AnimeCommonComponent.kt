package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeSortFilter
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.domain.exception.common.NoContentException
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.detail.toAnimeDetail
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeUser
import club.anifox.backend.domain.mappers.anime.toAnimeVideo
import club.anifox.backend.domain.mappers.anime.toGenre
import club.anifox.backend.domain.mappers.anime.toStudio
import club.anifox.backend.domain.model.anime.AnimeFranchise
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeRelation
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.episode.AnimeEpisode
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.jpa.entity.anime.AnimeBlockedTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeFranchiseTable
import club.anifox.backend.jpa.entity.anime.common.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.common.AnimeVideoTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeScheduleTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.entity.anime.episodes.EpisodeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.jpa.repository.user.anime.UserProgressAnimeRepository
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.AnimeUtils
import club.anifox.backend.util.user.UserUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Autowired
    private lateinit var userProgressAnimeRepository: UserProgressAnimeRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    @Autowired
    private lateinit var userUtils: UserUtils

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

        root.fetch<AnimeTable, Any>("similar", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        } else {
            val similarAnimeList =
                anime[0].similar.map { similar ->
                    similar.similarAnime.toAnimeLight()
                }

            if (similarAnimeList.isNotEmpty()) {
                return similarAnimeList
            }

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
            val relatedAnimeList =
                anime[0].related.map { relation ->
                    AnimeRelationLight(
                        relation.relatedAnime.toAnimeLight(),
                        AnimeRelation(
                            type = relation.type,
                        ),
                    )
                }

            if (relatedAnimeList.isNotEmpty()) {
                return relatedAnimeList
            }

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

        val anime =
            entityManager
                .createQuery(criteriaQuery)
                .resultList
                .firstOrNull() ?: throw NotFoundException("Anime with url = $url not found")

        if (anime.screenshots.isNotEmpty()) {
            return anime.screenshots
        }

        throw NoContentException("There are no screenshots")
    }

    fun getAnimeVideos(
        url: String,
        type: AnimeVideoType?,
    ): List<AnimeVideo> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeVideoTable> = criteriaBuilder.createQuery(AnimeVideoTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val videosJoin = animeRoot.join<AnimeTable, AnimeVideoTable>("videos", JoinType.LEFT)

        val predicates =
            mutableListOf(
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

    fun getAnimeEpisodes(
        token: String?,
        url: String,
        page: Int,
        limit: Int,
        sort: AnimeSortFilter?,
        translationId: Int?,
        searchQuery: String?,
    ): List<AnimeEpisode> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeTable> = criteriaBuilder.createQuery(AnimeEpisodeTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)
        val episodesJoin: Join<AnimeTable, AnimeEpisodeTable> = animeRoot.join("episodes", JoinType.LEFT)

        criteriaQuery.select(episodesJoin)

        val predicates = mutableListOf<Predicate>()

        predicates.add(criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url))

        if (translationId != null) {
            val translationsJoin = episodesJoin.join<AnimeEpisodeTable, EpisodeTranslationTable>("translations", JoinType.LEFT)
            predicates.add(criteriaBuilder.equal(translationsJoin.get<AnimeTranslationTable>("translation").get<Int>("id"), translationId))
        }

        if (!searchQuery.isNullOrEmpty()) {
            val titlePredicate = criteriaBuilder.like(episodesJoin.get<String>("title"), "%$searchQuery%")
            val titleEnPredicate = criteriaBuilder.like(episodesJoin.get<String>("titleEn"), "%$searchQuery%")
            val numberPredicate = criteriaBuilder.equal(episodesJoin.get<Int>("number"), searchQuery.toIntOrNull() ?: -1)

            predicates.add(criteriaBuilder.or(titlePredicate, titleEnPredicate, numberPredicate))
        }

        criteriaQuery.where(*predicates.toTypedArray())

        when (sort) {
            AnimeSortFilter.Asc -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
            AnimeSortFilter.Desc -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
            else -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
        }

        val query = entityManager.createQuery(criteriaQuery)
        val firstResult = (page - 1) * limit
        query.firstResult = if (firstResult >= 0) firstResult else 0
        query.maxResults = limit

        val episodes: List<AnimeEpisodeTable> = query.resultList

        return if (token != null) {
            val user = userUtils.checkUser(token)
            val userProgress = userProgressAnimeRepository.findByUserAndAnime(user, anime)
            val progressMap = userProgress.associateBy { it.episode.id }

            episodes.map { episode ->
                val timing = progressMap[episode.id]?.timing ?: 0.0
                episode.toAnimeEpisodeUser(timing)
            }
        } else {
            episodes.map { it.toAnimeEpisodeLight() }
        }
    }

    /*
        TODO: REWORK BLOCKED REQUEST
     */
    @Transactional
    fun blockAnime(
        url: String?,
        shikimoriId: Int?,
    ) {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        val predicate =
            when {
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

            val animeBlocked =
                AnimeBlockedTable(
                    shikimoriID = animeEntity.shikimoriId,
                )

            animeBlockedRepository.saveAndFlush(animeBlocked)
        } else {
            if (shikimoriId != null) {
                val animeBlocked =
                    AnimeBlockedTable(
                        shikimoriID = shikimoriId,
                    )

                animeBlockedRepository.saveAndFlush(animeBlocked)
            }
        }
    }

    fun getAnimeFranchise(
        url: String,
        type: AnimeRelationFranchise?,
    ): List<AnimeFranchise> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeFranchiseTable> = criteriaBuilder.createQuery(AnimeFranchiseTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val franchiseJoin = animeRoot.join<AnimeTable, AnimeFranchiseTable>("franchiseMultiple", JoinType.LEFT)
        val targetJoin = franchiseJoin.join<AnimeFranchiseTable, AnimeTable>("target", JoinType.INNER)

        val predicates =
            mutableListOf(
                criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url),
            )

        if (type != null) {
            predicates.add(criteriaBuilder.equal(franchiseJoin.get<AnimeRelationFranchise>("relationType"), type))
        }

        criteriaQuery.select(franchiseJoin)
        criteriaQuery.where(*predicates.toTypedArray())

        val results = entityManager.createQuery(criteriaQuery).resultList

        if (results.isEmpty()) {
            throw NoContentException("There are no franchise relations for this anime")
        }

        return results.map { franchise ->
            AnimeFranchise(
                anime = franchise.target.toAnimeLight(),
                relation = franchise.relationTypeRus,
                targetUrl = franchise.target.url,
            )
        }
    }

    fun getWeeklySchedule(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        page: Int,
        limit: Int,
        dayOfWeek: DayOfWeek? = null,
    ): Map<String, List<AnimeLight>> {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeScheduleTable> =
            criteriaBuilder.createQuery(AnimeEpisodeScheduleTable::class.java)

        val scheduleRoot: Root<AnimeEpisodeScheduleTable> = criteriaQuery.from(AnimeEpisodeScheduleTable::class.java)
        val animeJoin = scheduleRoot.join<AnimeEpisodeScheduleTable, AnimeTable>("anime", JoinType.INNER)

        val predicates = mutableListOf<Predicate>()

        // Добавляем фильтр по датам
        if (startDate != null && endDate != null) {
            predicates.add(
                criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(scheduleRoot.get("nextEpisodeDate"), startDate),
                    criteriaBuilder.lessThanOrEqualTo(scheduleRoot.get("nextEpisodeDate"), endDate),
                ),
            )
        }

        // Добавляем фильтр по дню недели в сам запрос
        dayOfWeek?.let {
            predicates.add(criteriaBuilder.equal(scheduleRoot.get<DayOfWeek>("dayOfWeek"), it))
        }

        criteriaQuery.where(*predicates.toTypedArray())

        // Добавляем сортировку по ID для стабильности результатов
        criteriaQuery.orderBy(
            criteriaBuilder.asc(scheduleRoot.get<DayOfWeek>("dayOfWeek")),
            criteriaBuilder.asc(scheduleRoot.get<LocalDateTime>("nextEpisodeDate")),
            criteriaBuilder.asc(scheduleRoot.get<Long>("id")), // Добавляем сортировку по ID
        )

        // Применяем пагинацию
        val query = entityManager.createQuery(criteriaQuery)
        val firstResult = (page - 1) * limit
        query.firstResult = if (firstResult >= 0) firstResult else 0
        query.maxResults = limit

        val schedules = query.resultList

        // Формируем результат
        return if (dayOfWeek != null) {
            mapOf(
                dayOfWeek.toString().lowercase() to schedules.map { it.anime.toAnimeLight() },
            )
        } else {
            // Группируем результаты по дням недели
            schedules.groupBy {
                it.dayOfWeek.toString().lowercase()
            }.mapValues { (_, scheduleList) ->
                scheduleList.map { it.anime.toAnimeLight() }
            }
        }
    }
}
