package club.anifox.backend.service.anime.translate

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.translate.edge.TranslateTextDto
import club.anifox.backend.domain.dto.translate.edge.TranslatedTextDto
import club.anifox.backend.domain.model.translate.TranslatedText
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TranslateComponent {

    @Autowired
    private lateinit var client: HttpClient

    suspend fun translateText(text: List<TranslateTextDto>): List<TranslatedText> {
        if (text.isEmpty()) return emptyList()
        return translateWithRetry(text)
            .map { it.translations.map { t -> TranslatedText(t.text) } }
            .flatten()
    }

    private suspend fun translateWithRetry(
        text: List<TranslateTextDto>,
    ): List<TranslatedTextDto> {
        return try {
            translateRequest(text)
        } catch (e: Exception) {
            delay(1500)
            translateRequest(text)
        }
    }

    private suspend fun translateRequest(text: List<TranslateTextDto>): List<TranslatedTextDto> {
        return client.post {
            bearerAuth(
                client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.EDGE
                        encodedPath = "${Constants.EDGE_TRANSLATE}${Constants.EDGE_AUTH}"
                    }
                    header("Accept", "application/vnd.api+json")
                }.bodyAsText(),
            )
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.MICROSOFT
                encodedPath = Constants.MICROSOFT_TRANSLATE
            }
            setBody(text)
            header("Accept", "application/vnd.api+json")
            parameter("from", "en")
            parameter("to", "ru")
            parameter("api-version", "3.0")
        }.body<List<TranslatedTextDto>>()
    }
}
