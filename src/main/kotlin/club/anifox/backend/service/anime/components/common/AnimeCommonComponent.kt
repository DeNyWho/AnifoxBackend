package club.anifox.backend.service.anime.components.common

import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeDefaultFilter
import club.anifox.backend.domain.exception.common.NoContentException
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.detail.toAnimeDetail
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeHistory
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeUser
import club.anifox.backend.domain.mappers.anime.toAnimeVideo
import club.anifox.backend.domain.mappers.anime.toGenre
import club.anifox.backend.domain.mappers.anime.toStudio
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeRelation
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.character.AnimeCharacterLight
import club.anifox.backend.domain.model.anime.character.AnimeCharacterResponse
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.episode.AnimeEpisode
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeHistory
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.sitemap.AnimeSitemap
import club.anifox.backend.domain.model.anime.statistics.AnimeStatistics
import club.anifox.backend.domain.model.anime.statistics.AnimeStatisticsScore
import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.AnimeFavoriteStatusDistributionTable
import club.anifox.backend.jpa.entity.anime.AnimeRatingDistributionTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
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
import club.anifox.backend.util.anime.AnimeUtils
import club.anifox.backend.util.user.UserUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
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
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeGenreTable, Any>("genres", JoinType.LEFT)
        root.fetch<AnimeImagesTable, Any>("images", JoinType.LEFT)
        root.fetch<AnimeStudioTable, Any>("studios", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList
        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        } else {
            return anime.first().toAnimeDetail()
        }
    }

    fun getAnimeStatistics(url: String): AnimeStatistics {
        val anime = animeUtils.checkAnime(url)

        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaFavQuery = criteriaBuilder.createQuery(AnimeFavoriteStatusDistributionTable::class.java)
        val rootFav = criteriaFavQuery.from(AnimeFavoriteStatusDistributionTable::class.java)

        rootFav.fetch<AnimeFavoriteStatusDistributionTable, AnimeTable>("anime", JoinType.INNER)

        criteriaFavQuery.where(criteriaBuilder.equal(rootFav.get<AnimeTable>("anime").get<String>("id"), anime.id))

        val criteriaRatQuery = criteriaBuilder.createQuery(AnimeRatingDistributionTable::class.java)
        val rootRat = criteriaRatQuery.from(AnimeRatingDistributionTable::class.java)

        rootRat.fetch<AnimeRatingDistributionTable, AnimeTable>("anime", JoinType.INNER)

        criteriaRatQuery.where(criteriaBuilder.equal(rootRat.get<AnimeTable>("anime").get<String>("id"), anime.id))

        val fav = entityManager.createQuery(criteriaFavQuery).resultList
        val rat = entityManager.createQuery(criteriaRatQuery).resultList

        if (fav.isNotEmpty()) {
            return AnimeStatistics(
                watching = fav.firstOrNull()?.watching ?: 0,
                completed = fav.firstOrNull()?.completed ?: 0,
                onHold = fav.firstOrNull()?.onHold ?: 0,
                dropped = fav.firstOrNull()?.dropped ?: 0,
                planToWatch = fav.firstOrNull()?.planToWatch ?: 0,
                total = fav.firstOrNull()?.total ?: 0,
                scores = (1..10).map { score ->
                    AnimeStatisticsScore(score, rat.firstOrNull()?.getScoreCount(score) ?: 0)
                },
            )
        }
        throw IllegalStateException("Anime statistics not found")
    }

    @Transactional()
    fun getAnimeExternalLinks(url: String): List<AnimeExternalLinksTable> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeExternalLinksTable, Any>("externalLinks", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        } else {
            val externalLinks = anime.first().externalLinks.toList()

            if (externalLinks.isNotEmpty()) {
                return externalLinks
            }

            throw NoContentException("External links not found")
        }
    }

    fun getAnimeCharactersWithRoles(
        page: Int,
        limit: Int,
        url: String,
        role: String?,
        search: String?,
    ): AnimeCharacterResponse {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList
        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        }

        // Get available roles
        val availableRolesQuery = criteriaBuilder.createQuery(String::class.java)
        val rootRole = availableRolesQuery.from(AnimeCharacterRoleTable::class.java)
        availableRolesQuery.select(rootRole.get("role"))
            .where(criteriaBuilder.equal(rootRole.get<AnimeTable>("anime").get<String>("id"), anime.first().id))
            .distinct(true)
        val availableRoles = entityManager.createQuery(availableRolesQuery).resultList

        // Get characters
        val characterQuery = criteriaBuilder.createQuery(AnimeCharacterLight::class.java)
        val rootCharacter: Root<AnimeCharacterRoleTable> = characterQuery.from(AnimeCharacterRoleTable::class.java)
        val characterJoin: Join<AnimeCharacterRoleTable, AnimeCharacterTable> = rootCharacter.join("character")

        characterQuery.select(
            criteriaBuilder.construct(
                AnimeCharacterLight::class.java,
                characterJoin.get<String>("id"),
                rootCharacter.get<String>("role"),
                characterJoin.get<String>("image"),
                characterJoin.get<String>("name"),
            ),
        )

        val predicates = mutableListOf<Predicate>()
        predicates.add(
            criteriaBuilder.equal(rootCharacter.get<AnimeTable>("anime").get<String>("id"), anime.first().id),
        )
        role?.let {
            predicates.add(criteriaBuilder.equal(rootCharacter.get<String>("role"), it))
        }
        search?.let {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(characterJoin.get("name")), "%" + it.lowercase() + "%"))
        }

        characterQuery.where(criteriaBuilder.and(*predicates.toTypedArray()))

        val mainRolePriority: Expression<Int> = criteriaBuilder.selectCase<Int>()
            .`when`(criteriaBuilder.equal(rootCharacter.get<String>("role"), "Главная"), 1)
            .otherwise(2)

        characterQuery.orderBy(
            criteriaBuilder.asc(mainRolePriority),
            criteriaBuilder.asc(characterJoin.get<String>("name")),
        )

        val query = entityManager.createQuery(characterQuery)
        query.firstResult = page * limit
        query.maxResults = limit

        return AnimeCharacterResponse(
            characters = query.resultList,
            availableRoles = availableRoles,
        )
    }

    fun getAnimeSimilar(page: Int, limit: Int, url: String): List<AnimeLight> {
        val animeId = entityManager.createQuery(
            "SELECT a.id FROM AnimeTable a WHERE a.url = :url",
            String::class.java,
        )
            .setParameter("url", url)
            .singleResult

        val query = entityManager.createQuery(
            "SELECT s.similarAnime FROM AnimeSimilarTable s WHERE s.anime.id = :animeId",
            AnimeTable::class.java,
        )
            .setParameter("animeId", animeId)
            .setFirstResult(page * limit)
            .setMaxResults(limit)

        return query.resultList.map {
            it.toAnimeLight()
        }
    }

    fun getAnimeRelated(url: String): List<AnimeRelationLight> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeGenreTable, Any>("genres", JoinType.LEFT)
        root.fetch<AnimeImagesTable, Any>("images", JoinType.LEFT)
        root.fetch<AnimeStudioTable, Any>("studios", JoinType.LEFT)
        root.fetch<AnimeTable, Any>("related", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
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

            throw NoContentException("Related anime not found")
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
                .firstOrNull() ?: throw NotFoundException("Anime not found")

        if (anime.screenshots.isNotEmpty()) {
            return anime.screenshots
        }

        throw NoContentException("Screenshots not found")
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

        val predicates = mutableListOf(
            criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url),
            criteriaBuilder.notEqual(videosJoin.get<AnimeVideoType>("type"), AnimeVideoType.MainTrailer),
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

        throw NoContentException("Videos not found")
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
        sort: AnimeDefaultFilter?,
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
            val titlePredicate = criteriaBuilder.like(episodesJoin.get("title"), "%$searchQuery%")
            val titleEnPredicate = criteriaBuilder.like(episodesJoin.get("titleEn"), "%$searchQuery%")
            val numberPredicate = criteriaBuilder.equal(episodesJoin.get<Int>("number"), searchQuery.toIntOrNull() ?: -1)

            predicates.add(criteriaBuilder.or(titlePredicate, titleEnPredicate, numberPredicate))
        }

        criteriaQuery.where(*predicates.toTypedArray())

        when (sort) {
            AnimeDefaultFilter.Asc -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
            AnimeDefaultFilter.Desc -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
            else -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
        }

        val query = entityManager.createQuery(criteriaQuery)
        val firstResult = page * limit
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

    fun getAnimeEpisodesHistory(
        url: String,
        page: Int,
        limit: Int,
    ): List<AnimeEpisodeHistory> {
        val anime: AnimeTable = animeUtils.checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeTable> = criteriaBuilder.createQuery(AnimeEpisodeTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)
        val episodesJoin: Join<AnimeTable, AnimeEpisodeTable> = animeRoot.join("episodes", JoinType.LEFT)

        criteriaQuery.select(episodesJoin)

        val predicates = mutableListOf<Predicate>()

        predicates.add(criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url))
        criteriaQuery.where(*predicates.toTypedArray())

        criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))

        val query = entityManager.createQuery(criteriaQuery)
        val firstResult = page * limit
        query.firstResult = if (firstResult >= 0) firstResult else 0
        query.maxResults = limit

        val episodes: MutableList<AnimeEpisodeHistory> = query.resultList.map { it.toAnimeEpisodeHistory() }.toMutableList()
        val nextEpisode = anime.nextEpisode

        if (nextEpisode != null && episodes.isNotEmpty()) {
            val newEpisode = AnimeEpisodeHistory(
                title = "Новая серия",
                number = episodes.maxOf { it.number } + 1,
                aired = nextEpisode.toLocalDate(),
            )

            if (page == 0 && anime.status == AnimeStatus.Ongoing) {
                episodes.add(0, newEpisode)
            }

            return episodes
        }

        throw NotFoundException("Episodes not found")
    }

    fun getWeeklySchedule(
        page: Int,
        limit: Int,
        date: LocalDate,
        maxPerDay: Int = 10,
    ): Map<String, List<AnimeLight>> {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeScheduleTable> =
            criteriaBuilder.createQuery(AnimeEpisodeScheduleTable::class.java)

        val scheduleRoot: Root<AnimeEpisodeScheduleTable> = criteriaQuery.from(AnimeEpisodeScheduleTable::class.java)
        val animeJoin = scheduleRoot.join<AnimeEpisodeScheduleTable, AnimeTable>("anime", JoinType.INNER)
        animeJoin.join<AnimeTable, AnimeGenreTable>("genres", JoinType.LEFT)
        animeJoin.join<AnimeTable, AnimeImagesTable>("images", JoinType.LEFT)
        animeJoin.join<AnimeTable, AnimeStudioTable>("studios", JoinType.LEFT)

        val predicates = mutableListOf<Predicate>()

        val allDates = date.let { inputDate ->
            val currentDayOfWeek = inputDate.dayOfWeek

            (1..7).map { dayNumber ->
                val targetDate = when {
                    dayNumber < currentDayOfWeek.value ->
                        inputDate.plusWeeks(1).with(DayOfWeek.of(dayNumber))
                    else ->
                        inputDate.with(DayOfWeek.of(dayNumber))
                }
                targetDate
            }
        }

        if (allDates.isNotEmpty()) {
            val weekPredicates = allDates.map { targetDate ->
                val startOfDay = targetDate.atStartOfDay()
                val endOfDay = targetDate.atTime(23, 59, 59)

                criteriaBuilder.or(
                    // Проверяем nextEpisodeDate
                    criteriaBuilder.between(
                        scheduleRoot.get<LocalDateTime>("nextEpisodeDate"),
                        criteriaBuilder.literal(startOfDay),
                        criteriaBuilder.literal(endOfDay),
                    ),
                    // Проверяем previousEpisodeDate для тех аниме, которые уже обновились
                    criteriaBuilder.and(
                        criteriaBuilder.between(
                            scheduleRoot.get<LocalDateTime>("previousEpisodeDate"),
                            criteriaBuilder.literal(startOfDay),
                            criteriaBuilder.literal(endOfDay),
                        ),
                        criteriaBuilder.isNotNull(scheduleRoot.get<LocalDateTime>("nextEpisodeDate")),
                    ),
                )
            }

            predicates.add(criteriaBuilder.or(*weekPredicates.toTypedArray()))
        }

        criteriaQuery.where(*predicates.toTypedArray())
        criteriaQuery.orderBy(
            criteriaBuilder.asc(scheduleRoot.get<LocalDateTime>("nextEpisodeDate")),
        )

        val allSchedules = entityManager.createQuery(criteriaQuery).resultList

        // Группируем расписание по дням, учитывая как nextEpisodeDate, так и previousEpisodeDate
        val groupedSchedules = allSchedules.groupBy { schedule ->
            when {
                // Если previousEpisodeDate попадает в нужный день и уже есть nextEpisodeDate,
                // значит аниме уже обновилось и должно отображаться в этот день
                allDates.any { it == schedule.previousEpisodeDate?.toLocalDate() } &&
                    schedule.nextEpisodeDate != null -> schedule.previousEpisodeDate?.toLocalDate()
                // Иначе группируем по nextEpisodeDate
                else -> schedule.nextEpisodeDate?.toLocalDate()
            }
        }.mapValues { (_, schedules) ->
            schedules
                .distinctBy { it.anime.id }
                .take(maxPerDay)
        }

        val daysOrder = listOf(
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
        )

        val startDayIndex = maxOf(0, page * limit)
        val endDayIndex = minOf(startDayIndex + limit, daysOrder.size)

        return daysOrder
            .slice(startDayIndex until endDayIndex)
            .associate { (dayName, dayOfWeek) ->
                val dateOfWeek = allDates.find { it.dayOfWeek == dayOfWeek }
                dayName to (
                    dateOfWeek?.let { date ->
                        groupedSchedules[date]?.map { schedule ->
                            schedule.anime.toAnimeLight()
                        }
                    } ?: emptyList()
                    )
            }
    }

    fun getAnimeSitemap(): List<AnimeSitemap> {
        return entityManager.createQuery(
            """
            SELECT new club.anifox.backend.domain.model.anime.sitemap.AnimeSitemap(a.url, a.updatedAt) FROM AnimeTable a
            """.trimIndent(),
            AnimeSitemap::class.java,
        )
            .resultList
    }
}
