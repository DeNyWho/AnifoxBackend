package club.anifox.backend.service.anime.components.translate

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.translate.edge.TranslateTextDto
import club.anifox.backend.domain.dto.translate.edge.TranslatedTextDto
import club.anifox.backend.domain.model.translate.TranslatedText
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TranslateComponent {
    @Autowired
    private lateinit var client: HttpClient

    // Только для токена авторизации
    private var authToken: String? = null
    private var tokenExpirationTime: Long = 0
    private val authMutex = Mutex()

    suspend fun translateChunks(
        texts: List<TranslateTextDto>,
        chunkSize: Int = 10,
    ): List<TranslatedText> = coroutineScope {
        texts.chunked(chunkSize)
            .map { chunk ->
                async(Dispatchers.IO) {
                    translateText(chunk)
                }
            }
            .awaitAll()
            .flatten()
    }

    suspend fun translateText(text: List<TranslateTextDto>): List<TranslatedText> {
        if (text.isEmpty()) return emptyList()

        return translateWithRetry(text)
            .map { it.translations.map { t -> TranslatedText(t.text) } }
            .flatten()
    }

    suspend fun translateSingleText(text: String): String {
        var result: String? = null
        while (result == null || result == "null") {
            result = translateWithRetry(listOf(TranslateTextDto(text)))
                .map { it.translations.map { t -> TranslatedText(t.text) } }
                .flatten()
                .firstOrNull()
                ?.text
        }
        return result
    }

    private suspend fun translateWithRetry(text: List<TranslateTextDto>): List<TranslatedTextDto> {
        return try {
            translateRequest(text)
        } catch (e: Exception) {
            // Сбрасываем токен при ошибке, возможно он истёк
            authToken = null
            translateRequest(text)
        }
    }

    private suspend fun getAuthToken(): String = authMutex.withLock {
        val currentTime = System.currentTimeMillis()

        // Проверяем, не истёк ли токен
        if (authToken == null || currentTime >= tokenExpirationTime) {
            val response = client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.EDGE
                    encodedPath = "${Constants.EDGE_TRANSLATE}${Constants.EDGE_AUTH}"
                }
                header("Accept", "application/vnd.api+json")
            }

            authToken = response.bodyAsText()
            // Устанавливаем время жизни токена (например, на 8 минут)
            tokenExpirationTime = currentTime + 8 * 60 * 1000
        }

        return authToken!!
    }

    private suspend fun translateRequest(text: List<TranslateTextDto>): List<TranslatedTextDto> {
        return client.post {
            bearerAuth(getAuthToken())
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
