package club.anifox.backend.service.anime.components.update

import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.service.anime.components.block.AnimeBlockComponent
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.parser.FetchImageComponent
import club.anifox.backend.service.anime.components.schedule.AnimeScheduleComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hibernate.jpa.AvailableHints
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Component
class AnimeUpdateComponent(
    private val animeErrorParserRepository: AnimeErrorParserRepository,
    private val animeGenreRepository: AnimeGenreRepository,
    private val fetchImageComponent: FetchImageComponent,
    private val entityManager: EntityManager,
    private val episodesComponent: EpisodesComponent,
    private val shikimoriComponent: AnimeShikimoriComponent,
    private val animeRepository: AnimeRepository,
    private val animeBlockComponent: AnimeBlockComponent,
    private val animeScheduleComponent: AnimeScheduleComponent,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика", "Юри", "юри")
    private val genreCache = ConcurrentHashMap<String, AnimeGenreTable>()

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun update(onlyOngoing: Boolean = false) {
        runBlocking {
            try {
                logger.info("Starting update process with onlyOngoing=$onlyOngoing")
                val currentYear = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().year
                logger.info("Fetching shikimoriIds for year $currentYear")

                val shikimoriIds = fetchShikimoriIds(currentYear, onlyOngoing)
                logger.info("Found ${shikimoriIds.size} anime to update")

                var processedCount = 0
                val totalCount = shikimoriIds.size

                shikimoriIds.chunked(BATCH_SIZE).forEach { batchIds ->
                    try {
                        logger.info("Processing batch ${processedCount + 1} of ${totalCount / BATCH_SIZE}")
                        processBatch(batchIds)
                        processedCount += batchIds.size
                        logger.info("Completed $processedCount/$totalCount anime updates")
                    } catch (e: Exception) {
                        logger.error("Failed to process batch ${batchIds.joinToString()}", e)
                    }
                }
                entityManager.clear()

                logger.info("Update process completed. Processed $processedCount anime.")
            } catch (e: Exception) {
                logger.error("Critical error in update process", e)
                throw e
            }
        }
    }

    private suspend fun fetchShikimoriIds(currentYear: Int, onlyOngoing: Boolean): List<Int> {
        return coroutineScope {
            try {
                val query = if (onlyOngoing) {
                    """
                    SELECT a.shikimoriId FROM AnimeTable a
                    WHERE a.year BETWEEN :prevYear AND :currentYear
                    AND a.status = :status
                    """.trimIndent()
                } else {
                    """
                    SELECT a.shikimoriId FROM AnimeTable a
                    WHERE a.year BETWEEN :prevYear AND :currentYear
                    """.trimIndent()
                }

                withContext(Dispatchers.Default) {
                    entityManager.createQuery(query, Int::class.java)
                        .setParameter("prevYear", currentYear - 1)
                        .setParameter("currentYear", currentYear)
                        .apply {
                            if (onlyOngoing) {
                                setParameter("status", AnimeStatus.Ongoing)
                            }
                        }
                        .resultList
                }
            } catch (e: Exception) {
                logger.error("Failed to fetch shikimoriIds", e)
                emptyList()
            }
        }
    }

    @Transactional
    private suspend fun processBatch(batchIds: List<Int>) = coroutineScope {
        try {
            val animeList = fetchAnimeList(batchIds)
            logger.info("Processing batch of ${animeList.size} anime")

            val updatedAnime = animeList.map { anime ->
                async {
                    try {
                        processAnime(anime)
                    } catch (e: Exception) {
                        logger.error("Failed to process anime ${anime.shikimoriId}", e)
                        logError(anime.shikimoriId, "PROCESS_FAILED", e.message)
                        null // Возвращаем null, чтобы продолжить обработку других аниме
                    }
                }
            }.awaitAll().filterNotNull()

            if (updatedAnime.isNotEmpty()) {
                try {
                    animeRepository.saveAllAndFlush(updatedAnime)
                } catch (e: Exception) {
                    logger.error("Failed to save updated anime batch", e)
                    updatedAnime.forEach { anime ->
                        logError(anime.shikimoriId, "SAVE_FAILED", e.message)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process batch $batchIds", e)
        }
    }

    private suspend fun fetchAnimeList(batchIds: List<Int>): List<AnimeTable> = coroutineScope {
        withContext(Dispatchers.Default) {
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(AnimeTable::class.java)
            val root = query.from(AnimeTable::class.java)

            root.fetch<AnimeTable, AnimeEpisodeTable>("episodes", JoinType.LEFT)
            root.fetch<AnimeTable, AnimeGenreTable>("genres", JoinType.LEFT)
            root.fetch<AnimeTable, MutableList<String>>("screenshots", JoinType.LEFT)
            root.fetch<AnimeTable, AnimeImagesTable>("images", JoinType.LEFT)
            root.fetch<AnimeTable, AnimeTranslationTable>("translations", JoinType.LEFT)
            root.fetch<AnimeTable, AnimeEpisodeTranslationCountTable>("translationsCountEpisodes", JoinType.LEFT)

            query.select(root).where(root.get<Int>("shikimoriId").`in`(batchIds)).distinct(true)

            entityManager.createQuery(query)
                .setHint(AvailableHints.HINT_FETCH_SIZE, BATCH_SIZE)
                .resultList
        }
    }

    private suspend fun processAnime(anime: AnimeTable): AnimeTable? = coroutineScope {
        try {
            val shikimori = shikimoriComponent.fetchAnime(anime.shikimoriId) ?: run {
                logError(anime.shikimoriId, "NO_SHIKIMORI_DATA", "No Shikimori data found")
                return@coroutineScope null
            }

            val licensors = shikimori.licensors.filter { it == "DEEP" || it == "Экспонента" || it == "Вольга" }
            val isLicensed = licensors.isNotEmpty()

            if (!isLicensed) {
                val updatedAnime = updateAnimeDetails(anime, shikimori)
                updatedAnime
            } else {
                animeBlockComponent.blockAnime(anime.url)
                logError(anime.shikimoriId, "ANIME_UPDATE_FAILED", "THIS ANIME IS BLOCKED")
                null
            }
        } catch (e: Exception) {
            logError(anime.shikimoriId, "UPDATE_FAILED", e.message)
            null
        }
    }

    private suspend fun updateAnimeDetails(anime: AnimeTable, shikimori: ShikimoriDto): AnimeTable = coroutineScope {
        try {
            if (anime.screenshots.isEmpty()) {
                val screenshotsDeferred = async {
                    try {
                        fetchAndSaveScreenshots(shikimori, anime)
                    } catch (e: Exception) {
                        logError(anime.shikimoriId, "SCREENSHOTS_FETCH_FAILED", e.message)
                        emptyList()
                    }
                }
                anime.screenshots.addAll(screenshotsDeferred.await())
            }

            try {
                val cb = entityManager.criteriaBuilder
                val query = cb.createQuery(AnimeIdsTable::class.java)
                val root = query.from(AnimeIdsTable::class.java)

                root.fetch<AnimeIdsTable, AnimeTable>("anime", JoinType.INNER)

                query.where(cb.equal(root.get<AnimeTable>("anime").get<String>("id"), anime.id))

                val ids = entityManager.createQuery(query).resultList

                val criteriaQueryEpisodes: CriteriaQuery<AnimeEpisodeTable> = cb.createQuery(AnimeEpisodeTable::class.java)

                val animeRoot: Root<AnimeTable> = criteriaQueryEpisodes.from(AnimeTable::class.java)
                val episodesJoin: Join<AnimeTable, AnimeEpisodeTable> = animeRoot.join("episodes", JoinType.LEFT)
                criteriaQueryEpisodes.select(episodesJoin)
                criteriaQueryEpisodes.where(cb.equal(animeRoot.get<String>("id"), anime.id))
                val locallyEpisodes = entityManager.createQuery(criteriaQueryEpisodes).resultList

                val kitsuId = if (ids.isNotEmpty()) ids.first().kitsu.toString() else null

                val episodesDeferred = async {
                    episodesComponent.fetchEpisodes(
                        shikimoriId = anime.shikimoriId,
                        kitsuId = kitsuId,
                        type = anime.type,
                        urlLinkPath = anime.url,
                        defaultImage = anime.images.medium,
                        locallyEpisodes = locallyEpisodes,
                    )
                }

                val episodes = episodesDeferred.await()

                val translationsCount = episodesComponent.translationsCount(episodes)

                anime.apply {
                    this.addEpisodesAll(episodes)
                    this.addTranslation(translationsCount.map { it.translation })
                    this.addTranslationCount(translationsCount)

                    this.duration = this.duration?.let { duration ->
                        if (this.status == AnimeStatus.Released && episodes.size * 16 < duration) {
                            duration
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                logError(anime.shikimoriId, "EPISODES_UPDATE_FAILED", e.message)
                return@coroutineScope anime // Продолжаем с текущим anime
            }

            updateAnimeFromShikimori(anime, shikimori)
        } catch (e: Exception) {
            logError(anime.shikimoriId, "ANIME_UPDATE_FAILED", e.message ?: "FAILED ")
            throw e
        }
    }

    private suspend fun fetchAndSaveScreenshots(shikimori: ShikimoriDto, anime: AnimeTable): List<String> {
        return try {
            val screenshots = shikimoriComponent.fetchScreenshots(shikimori.id)
            screenshots.map { screenshot ->
                fetchImageComponent.saveImage(
                    screenshot,
                    CompressAnimeImageType.Screenshot,
                    anime.url,
                    true,
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch screenshots for anime ${anime.shikimoriId}: ${e.message}")
            emptyList()
        }
    }

    private suspend fun logError(shikimoriId: Int, cause: String, message: String?) {
        try {
            animeErrorParserRepository.saveAndFlush(
                AnimeErrorParserTable(
                    message = message ?: "FAILED",
                    cause = cause,
                    shikimoriId = shikimoriId,
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to log error for anime $shikimoriId", e)
        }
    }

    private fun updateAnimeFromShikimori(anime: AnimeTable, shikimori: ShikimoriDto): AnimeTable {
        val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.of("Europe/Moscow"))
        val genres =
            shikimori.genres
                .filter { it.russian !in inappropriateGenres }
                .map { genre ->
                    genreCache.computeIfAbsent(genre.russian) {
                        animeGenreRepository.findByGenre(genre.russian)
                            .orElseGet {
                                val newGenre = AnimeGenreTable(name = genre.russian)
                                animeGenreRepository.save(newGenre)
                                newGenre
                            }
                    }
                }

        return anime.apply {
            this.addAllAnimeGenre(genres)

            if (description.isEmpty()) {
                description = shikimori.description.ifEmpty { description }
                    .replace(Regex("\\[\\/?[a-z]+.*?\\]"), "")
            }

            shikimoriVotes = shikimori.usersRatesStats.sumOf { it.value }
            shikimoriRating = try {
                shikimori.score.toDouble()
            } catch (_: Exception) {
                0.0
            }

            status = when (shikimori.status) {
                "released" -> AnimeStatus.Released
                else -> AnimeStatus.Ongoing
            }

            animeScheduleComponent.updateSchedule(
                animeId = this.id,
                nextEpisodeDate = shikimori.nextEpisodeAt?.let { LocalDateTime.parse(it, formatterUpdated) },
            )

            val currentEpisodesSize = episodes.size
            if ((episodesAired ?: 0) < currentEpisodesSize) {
                updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
            }

            episodesCount = when {
                shikimori.episodes == 0 && status == AnimeStatus.Ongoing -> null
                shikimori.episodes < currentEpisodesSize -> currentEpisodesSize
                else -> maxOf(shikimori.episodes, currentEpisodesSize)
            }

            episodesAired = currentEpisodesSize
        }
    }

    private companion object {
        const val BATCH_SIZE = 2
    }
}
