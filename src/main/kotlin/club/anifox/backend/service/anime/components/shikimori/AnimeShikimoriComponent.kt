package club.anifox.backend.service.anime.components.shikimori

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriExternalLinksDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriFranchiseDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriRelationDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriScreenshotsDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriSimilarDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriVideoDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeShikimoriComponent {
    @Autowired
    private lateinit var client: HttpClient

    suspend fun fetchAnime(shikimoriId: Int): ShikimoriDto? {
        return runCatching {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                    contentType(ContentType.Text.Plain)
                    userAgent("AniFox")
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.SHIKIMORI
                    encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/$shikimoriId"
                }
            }.body<ShikimoriDto>()
        }.getOrNull()
    }

    suspend fun fetchVideos(shikimoriId: Int): List<ShikimoriVideoDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_VIDEOS}"
            }
        }.body<List<ShikimoriVideoDto>>()
    }

    suspend fun fetchScreenshots(shikimoriId: Int): List<String> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
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

    suspend fun fetchRelated(shikimoriId: Int): List<ShikimoriRelationDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_RELATED}"
            }
        }.body<List<ShikimoriRelationDto>>()
    }

    suspend fun fetchExternalLinks(shikimoriId: Int): List<ShikimoriExternalLinksDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_EXTERNAL_LINKS}"
            }
        }.body<List<ShikimoriExternalLinksDto>>()
    }

    suspend fun fetchSimilar(shikimoriId: Int): List<Int> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
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

    suspend fun fetchFranchise(shikimoriId: Int): ShikimoriFranchiseDto {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
                contentType(ContentType.Text.Plain)
                userAgent("AniFox")
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.SHIKIMORI
                encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${shikimoriId}${Constants.SHIKIMORI_FRANCHISE}"
            }
        }.body<ShikimoriFranchiseDto>()
    }
}
