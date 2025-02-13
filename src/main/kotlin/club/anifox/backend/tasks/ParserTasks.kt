package club.anifox.backend.tasks

import club.anifox.backend.service.anime.AnimeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("parser")
class ParserTasks {
    @Autowired
    private lateinit var animeService: AnimeService

    private val logger = LoggerFactory.getLogger(this::class.java)

//    @Scheduled(fixedDelay = 24, timeUnit = TimeUnit.HOURS)
//    fun parseAnime() {
//        animeService.parseTranslations(listOf(610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002, 1978, 1291, 1272, 1946))
//        animeService.parseAnime()
//    }

    @Scheduled(fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    fun parseAnimeIntegrations() {
        animeService.parseAnimeIntegrations()
    }

//    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
//    fun updateData() {
//        animeService.updateEpisodes(
//            onlyOngoing = false,
//        )
//    }
//
//    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
//    fun updateDataOngoingOnly() {
//        animeService.updateEpisodes(
//            onlyOngoing = true,
//        )
//    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun logHealthStatus() {
        logger.info("Parser health check - Active threads: ${Thread.activeCount()}")
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024

            logger.info(
                """
                Memory Usage:
                Used Memory: $usedMemory MB
                Free Memory: $freeMemory MB
                Total Memory: $totalMemory MB
                Max Memory: $maxMemory MB
                """.trimIndent(),
            )
        } catch (e: Exception) {
            logger.error("Failed to log memory statistics", e)
        }
    }
}
