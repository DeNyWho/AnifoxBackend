package club.anifox.backend

import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.keycloak.KeycloakService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AnifoxBackendApplicationTests {
    @Autowired
    private lateinit var client: HttpClient

    @Autowired
    private lateinit var kodikComponent: KodikComponent

    @Autowired
    private lateinit var keycloakService: KeycloakService

    @Autowired
    private lateinit var animeTranslationRepository: AnimeTranslationRepository

    @Test
    fun contextLoads() {
        val translationsIds = animeTranslationRepository.findAll().map { it.id }.joinToString(", ")
        var ar =
            runBlocking {
                kodikComponent.checkKodikList(translationsIds)
            }

        while (ar.nextPage != null) {
            ar =
                runBlocking {
                    client.get(ar.nextPage!!) {
                        headers {
                            contentType(ContentType.Application.Json)
                        }
                    }.body()
                }
        }
    }

    @Test
    fun testKeycloak() {
        val b = keycloakService.findByEmail("example.example@gmail.com")
        println("B = $b")
    }
}
