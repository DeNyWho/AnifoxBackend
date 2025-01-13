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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class AnimeUpdateComponent(
    private val transactionTemplate: TransactionTemplate,
    private val animeErrorParserRepository: AnimeErrorParserRepository,
    private val fetchImageComponent: FetchImageComponent,
    private val entityManager: EntityManager,
    private val episodesComponent: EpisodesComponent,
    private val shikimoriComponent: AnimeShikimoriComponent,
    private val animeRepository: AnimeRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Async
    @Transactional
    fun update() = runBlocking {
        val currentYear = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime().year

        // Get all ongoing anime IDs in one query
        val shikimoriIds: List<Int> = entityManager.createQuery(
            """
            SELECT a.shikimoriId FROM AnimeTable a
            WHERE a.year BETWEEN :prevYear AND :currentYear
            AND a.status = :status
            """.trimIndent(),
            Int::class.java,
        )
            .setParameter("prevYear", currentYear - 1)
            .setParameter("currentYear", currentYear)
            .setParameter("status", AnimeStatus.Ongoing)
            .resultList

        val supervisor = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Process in larger batches to reduce database load
        shikimoriIds.chunked(4).map { batchIds ->
            supervisor.async {
                try {
                    processAnimeBatch(batchIds)
                } catch (e: Exception) {
                    logger.error("Failed to process batch ${batchIds.joinToString()}", e)
                }
            }
        }.awaitAll()
    }

    @Transactional
    suspend fun processAnimeBatch(batchIds: List<Int>) {
        supervisorScope {
            try {
                val animeList = entityManager.createQuery(
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
                    .resultList

                logger.info("Processing batch of ${animeList.size} anime")

                // Fetch Shikimori data in parallel
                val shikimoriData = batchIds.map { shikimoriId ->
                    async { shikimoriComponent.fetchAnime(shikimoriId) }
                }.awaitAll()

                // Process each anime in the batch
                animeList.mapNotNull { anime ->
                    try {
                        val shikimori = shikimoriData.find { it?.id == anime.shikimoriId } ?: return@mapNotNull null

                        val readyToUpdate = updateAnime(anime, shikimori)
                        animeRepository.saveAndFlush(readyToUpdate)
                    } catch (e: Exception) {
                        logger.error("Failed to process anime ${anime.shikimoriId}", e)
                        animeErrorParserRepository.save(
                            AnimeErrorParserTable(
                                message = e.message,
                                cause = "UPDATE",
                                shikimoriId = anime.shikimoriId,
                            ),
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to process batch $batchIds: ${e.message}", e)
                throw e
            }
        }
    }

    private suspend fun updateAnime(anime: AnimeTable, shikimori: ShikimoriDto): AnimeTable {
        if (anime.screenshots.isEmpty()) {
            val screenshots = fetchAndSaveScreenshots(shikimori, anime)
            anime.screenshots.addAll(screenshots)
        }

        if (!anime.isLicensed) {
            val episodesReady = withContext(Dispatchers.IO) {
                episodesComponent.fetchEpisodes(
                    shikimoriId = anime.shikimoriId,
                    kitsuId = anime.ids.kitsu.toString(),
                    type = anime.type,
                    urlLinkPath = anime.url,
                    defaultImage = anime.images.medium,
                )
            }

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

        return updateAnimeFromShikimori(anime, shikimori)
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
}
