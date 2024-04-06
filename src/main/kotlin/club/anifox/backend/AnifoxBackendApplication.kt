package club.anifox.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AnifoxBackendApplication

fun main(args: Array<String>) {
    runApplication<AnifoxBackendApplication>(*args)
}
