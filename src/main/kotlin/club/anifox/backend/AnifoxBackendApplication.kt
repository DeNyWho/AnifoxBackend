package club.anifox.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableRetry
class AnifoxBackendApplication

fun main(args: Array<String>) {
    runApplication<AnifoxBackendApplication>(*args)
}
