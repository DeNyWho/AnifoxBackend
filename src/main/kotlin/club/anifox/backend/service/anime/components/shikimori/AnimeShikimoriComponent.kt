package club.anifox.backend.service.anime.components.shikimori

import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMediaDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeShikimoriComponent {

    @Autowired
    private lateinit var client: HttpClient

    fun checkShikimori(shikimoriId: String): ShikimoriMediaDto? {
        return try {
            runBlocking {
                client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "shikimori.me/api/animes/${shikimoriId}"
                    }
                }.body<ShikimoriMediaDto>()
            }
        } catch (e: Exception) {
            null
        }
    }
}
