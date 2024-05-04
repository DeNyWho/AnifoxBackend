package club.anifox.backend.service.anime.components.shikimori

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMediaDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriRelationDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriScreenshotsDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriSimilarDto
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

    fun checkShikimori(shikimoriId: Int): ShikimoriMediaDto? {
        return try {
            runBlocking {
                client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "shikimori.one/api/animes/$shikimoriId"
                    }
                }.body<ShikimoriMediaDto>()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchShikimoriScreenshots(shikimoriId: Int): List<String> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_SCREENSHOTS}"
            }
        }.body<List<ShikimoriScreenshotsDto>>().map { screenshot ->
            "https://${Constants.SHIKIMORI}${screenshot.original}"
        }
    }

    suspend fun fetchShikimoriRelated(shikimoriId: Int): List<ShikimoriRelationDto> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_RELATED}"
            }
        }.body<List<ShikimoriRelationDto>>()
    }

    suspend fun fetchShikimoriSimilar(shikimoriId: Int): List<Int> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_SIMILAR}"
            }
        }.body<List<ShikimoriSimilarDto>>().flatMap { similar ->
            listOfNotNull(similar.id)
        }.map { it }
    }
}
