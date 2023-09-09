package club.anifox.anifox_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AnifoxBackendApplication

fun main(args: Array<String>) {
    runApplication<AnifoxBackendApplication>(*args)
}
