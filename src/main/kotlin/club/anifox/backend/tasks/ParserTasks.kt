package club.anifox.backend.tasks

import club.anifox.backend.service.anime.AnimeService
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

//    @Scheduled(fixedDelay = 24, timeUnit = TimeUnit.HOURS)
//    fun parseAnime() {
//        animeService.parseTranslations(listOf(610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002, 1978, 1291, 1272, 1946))
//        animeService.parseAnime()
//    }

    @Scheduled(fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    fun parseAnimeIntegrations() {
        animeService.parseAnimeIntegrations()
    }

//    @Scheduled(fixedDelay = 1000)
//    fun updateData() {
//        animeService.updateEpisodes()
//    }
}
