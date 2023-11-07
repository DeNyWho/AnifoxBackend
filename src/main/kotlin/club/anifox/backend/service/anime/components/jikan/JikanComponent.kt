package club.anifox.backend.service.anime.components.jikan

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDefaultDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JikanComponent {

    @Autowired
    private lateinit var client: HttpClient

    suspend fun fetchJikanEpisodes(page: Int, shikimoriId: String): JikanResponseDefaultDto<JikanEpisodeDto> {
        delay(1000)
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${shikimoriId}${Constants.JIKAN_EPISODES}"
            }
            parameter("page", page)
        }.body<JikanResponseDefaultDto<JikanEpisodeDto>>()
    }
}
