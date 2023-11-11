package club.anifox.backend.service.anime.components.haglund

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.haglund.HaglundIdsDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class HaglundComponent {

    @Autowired
    private lateinit var client: HttpClient

    suspend fun fetchHaglundIds(shikimoriId: String): HaglundIdsDto {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.HAGLUND
                encodedPath = "${Constants.HAGLUND_API}${Constants.HAGLUND_VERSION}${Constants.HAGLUND_IDS}"
            }
            parameter("source", "myanimelist")
            parameter("id", shikimoriId)
        }.body<HaglundIdsDto>()
    }
}
