package club.anifox.backend

import club.anifox.backend.service.anime.AnimeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

//@Component
//class ScheduleTasks {
//
//    @Autowired
//    private lateinit var animeService: AnimeService
//
//    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
//    fun parseData() {
//        animeService.parseTranslations(listOf(610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002, 1978, 1291, 1272, 1946))
//        animeService.parseAnime()
//    }
//
////    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
////    fun checkBlocked() {
////        animeService.checkBlockedAnime()
////    }
//
//    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
//    fun updateData() {
//        animeService.updateEpisodes()
//    }
//}

@SpringBootApplication
@EnableScheduling
class AnifoxBackendApplication

fun main(args: Array<String>) {
    runApplication<AnifoxBackendApplication>(*args)
}
