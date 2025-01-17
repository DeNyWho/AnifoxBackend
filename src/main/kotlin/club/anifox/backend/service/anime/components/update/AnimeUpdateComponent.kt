package club.anifox.backend.service.anime.components.update

import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.parser.FetchImageComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.hibernate.jpa.AvailableHints
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
@OptIn(ExperimentalCoroutinesApi::class)
class AnimeUpdateComponent(
    private val animeErrorParserRepository: AnimeErrorParserRepository,
    private val fetchImageComponent: FetchImageComponent,
    private val entityManager: EntityManager,
    private val episodesComponent: EpisodesComponent,
    private val shikimoriComponent: AnimeShikimoriComponent,
    private val animeRepository: AnimeRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val limitedDispatcher = Dispatchers.IO.limitedParallelism(2)

    @Async
    @Transactional
    fun update(onlyOngoing: Boolean = false) = runBlocking(limitedDispatcher) {
        try {
            logger.info("Starting update process with onlyOngoing=$onlyOngoing")
            val currentYear = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().year
            logger.info("Fetching shikimoriIds for year $currentYear")

            val shikimoriIds = withContext(Dispatchers.IO) {
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

                    entityManager.createQuery(query, Int::class.java)
                        .setParameter("prevYear", currentYear - 1)
                        .setParameter("currentYear", currentYear)
                        .apply {
                            if (onlyOngoing) {
                                setParameter("status", AnimeStatus.Ongoing)
                            }
                        }
                        .resultList
                } catch (e: Exception) {
                    logger.error("Failed to fetch shikimoriIds", e)
                    throw e
                }
            }

            logger.info("Found ${shikimoriIds.size} anime to update")

            shikimoriIds.asSequence()
                .chunked(BATCH_SIZE)
                .forEach { batchIds ->
                    try {
                        logger.debug("Processing batch: ${batchIds.joinToString()}")
                        processBatchWithRetry(batchIds)
                        logger.debug("Batch completed: ${batchIds.joinToString()}")
                    } catch (e: Exception) {
                        logger.error("Failed to process batch ${batchIds.joinToString()}", e)
                        // Continue with next batch instead of stopping
                    } finally {
                        try {
                            entityManager.clear()
                        } catch (e: Exception) {
                            logger.error("Failed to clear entity manager", e)
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Critical error in update process", e)
            throw e
        }
    }

    private fun processBatchWithRetry(batchIds: List<Int>, retryCount: Int = 3) {
        batchIds.forEach { id ->
            var success = false
            repeat(retryCount) { attempt ->
                if (!success) {
                    try {
                        runBlocking(limitedDispatcher) {
                            processAnimeBatch(listOf(id))
                        }
                        success = true
                    } catch (e: Exception) {
                        logger.error("Failed to process anime ID $id (attempt ${attempt + 1}/$retryCount)", e)
                        if (attempt == retryCount - 1) {
                            // Save error after all retries failed
                            animeErrorParserRepository.save(
                                AnimeErrorParserTable(
                                    message = e.message,
                                    cause = "UPDATE BATCH FAILED ALL RETRIES",
                                    shikimoriId = id,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    @Transactional
    suspend fun processAnimeBatch(batchIds: List<Int>) {
        try {
            val animeList = withContext(Dispatchers.IO) {
                entityManager.createQuery(
                    """
                    SELECT DISTINCT a FROM AnimeTable a
                    LEFT JOIN FETCH a.episodes e
                    LEFT JOIN FETCH a.screenshots
                    LEFT JOIN FETCH a.ids
                    LEFT JOIN FETCH a.images
                    LEFT JOIN FETCH a.translations
                    LEFT JOIN FETCH a.schedule
                    LEFT JOIN FETCH a.translationsCountEpisodes
                    WHERE a.shikimoriId IN :ids
                    """.trimIndent(),
                    AnimeTable::class.java,
                )
                    .setParameter("ids", batchIds)
                    .setHint(AvailableHints.HINT_FETCH_SIZE, BATCH_SIZE)
                    .resultList
            }

            logger.info("Processing batch of ${animeList.size} anime")

            supervisorScope {
                // Process in smaller chunks with controlled memory usage
                animeList.chunked(2).forEach { chunk ->
                    val shikimoriData = chunk.map { anime ->
                        async(limitedDispatcher) {
                            try {
                                shikimoriComponent.fetchAnime(anime.shikimoriId)
                            } catch (e: Exception) {
                                logger.error("Failed to fetch Shikimori data for ID ${anime.shikimoriId}", e)
                                withContext(Dispatchers.IO) {
                                    animeErrorParserRepository.save(
                                        AnimeErrorParserTable(
                                            message = e.message,
                                            cause = "UPDATE SHIKIMORI FETCH FAILED",
                                            shikimoriId = anime.shikimoriId,
                                        ),
                                    )
                                }
                                null
                            }
                        }
                    }.awaitAll()

                    val updatedAnime = chunk.mapNotNull { anime ->
                        try {
                            val shikimori = shikimoriData.find { it?.id == anime.shikimoriId }
                            if (shikimori == null) {
                                logger.warn("No Shikimori data found for anime ${anime.shikimoriId}")
                                withContext(Dispatchers.IO) {
                                    animeErrorParserRepository.save(
                                        AnimeErrorParserTable(
                                            message = "No Shikimori data found",
                                            cause = "NO_SHIKIMORI_DATA",
                                            shikimoriId = anime.shikimoriId,
                                        ),
                                    )
                                }
                                return@mapNotNull null
                            }

                            withContext(limitedDispatcher) {
                                updateAnime(anime, shikimori)
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to process anime ${anime.shikimoriId}", e)
                            withContext(Dispatchers.IO) {
                                animeErrorParserRepository.save(
                                    AnimeErrorParserTable(
                                        message = e.message,
                                        cause = "UPDATE FAILED",
                                        shikimoriId = anime.shikimoriId,
                                    ),
                                )
                            }
                            null
                        }
                    }

                    if (updatedAnime.isNotEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                animeRepository.saveAllAndFlush(updatedAnime)
                            }
                            // Clear after save to free memory
                            entityManager.clear()
                        } catch (e: Exception) {
                            logger.error("Failed to save updated anime batch", e)
                            updatedAnime.forEach { anime ->
                                animeErrorParserRepository.save(
                                    AnimeErrorParserTable(
                                        message = e.message,
                                        cause = "SAVE UPDATE FAILED",
                                        shikimoriId = anime.shikimoriId,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process batch $batchIds: ${e.message}", e)
            throw e
        }
    }

    private suspend fun updateAnime(anime: AnimeTable, shikimori: ShikimoriDto): AnimeTable {
        try {
            if (anime.screenshots.isEmpty()) {
                try {
                    val screenshots = fetchAndSaveScreenshots(shikimori, anime)
                    anime.screenshots.addAll(screenshots)
                } catch (e: Exception) {
                    logger.error("Failed to fetch screenshots for anime ${anime.shikimoriId}", e)
                    animeErrorParserRepository.save(
                        AnimeErrorParserTable(
                            message = e.message,
                            cause = "UPDATE SCREENSHOTS FETCH FAILED",
                            shikimoriId = anime.shikimoriId,
                        ),
                    )
                }
            }

            if (!anime.isLicensed) {
                try {
                    supervisorScope {
                        val episodesReady = async {
                            episodesComponent.fetchEpisodes(
                                shikimoriId = anime.shikimoriId,
                                kitsuId = anime.ids.kitsu.toString(),
                                type = anime.type,
                                urlLinkPath = anime.url,
                                defaultImage = anime.images.medium,
                            )
                        }.await()

                        val translationsCountReady = episodesComponent.translationsCount(episodesReady)

                        anime.apply {
                            episodes.clear()
                            episodes.addAll(episodesReady)

                            translations.clear()
                            translations.addAll(translationsCountReady.map { it.translation }.toSet())

                            translationsCountEpisodes.clear()
                            translationsCountEpisodes.addAll(translationsCountReady)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update episodes for anime ${anime.shikimoriId}", e)
                    animeErrorParserRepository.save(
                        AnimeErrorParserTable(
                            message = e.message,
                            cause = "UPDATE EPISODES FAILED",
                            shikimoriId = anime.shikimoriId,
                        ),
                    )
                }
            }

            return updateAnimeFromShikimori(anime, shikimori)
        } catch (e: Exception) {
            logger.error("Failed to update anime ${anime.shikimoriId}", e)
            animeErrorParserRepository.save(
                AnimeErrorParserTable(
                    message = e.message,
                    cause = "ANIME_UPDATE_FAILED",
                    shikimoriId = anime.shikimoriId,
                ),
            )
            throw e
        }
    }

    private fun updateAnimeFromShikimori(anime: AnimeTable, shikimori: ShikimoriDto): AnimeTable {
        val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.of("Europe/Moscow"))

        return anime.apply {
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

            nextEpisode = shikimori.nextEpisodeAt?.let {
                LocalDateTime.parse(it, formatterUpdated)
            }

            when {
                anime.status == AnimeStatus.Ongoing && nextEpisode != null -> {
                    nextEpisode?.let { updateEpisodeSchedule(it) }
                }
                anime.status == AnimeStatus.Released && anime.schedule != null -> {
                    anime.updateEpisodeSchedule(null)
                }
            }

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
            }.toMutableList()
        } catch (e: Exception) {
            println("❌ Ошибка при загрузке скриншотов: ${e.message}")
            emptyList()
        }
    }

    private companion object {
        const val BATCH_SIZE = 2
    }
}
