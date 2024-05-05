package club.anifox.backend.service.anime.components.jikan

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.jikan.JikanDataDto
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDefaultDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDto
import club.anifox.backend.domain.dto.anime.jikan.JikanThemesDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JikanComponent {

    @Autowired
    private lateinit var client: HttpClient

    suspend fun fetchJikanEpisodes(page: Int, shikimoriId: Int): JikanResponseDefaultDto<JikanEpisodeDto> {
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${shikimoriId}${Constants.JIKAN_EPISODES}"
            }
            parameter("page", page)
        }.body<JikanResponseDefaultDto<JikanEpisodeDto>>()
    }

    suspend fun fetchJikanImages(shikimoriId: Int): JikanResponseDto<JikanDataDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/$shikimoriId"
            }
        }.body<JikanResponseDto<JikanDataDto>>()
    }

    suspend fun fetchJikanThemes(shikimoriId: Int): JikanResponseDto<JikanThemesDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${shikimoriId}${Constants.JIKAN_THEMES}"
            }
        }.body<JikanResponseDto<JikanThemesDto>>()
    }

    fun themesNormalize(input: String): String {
        val regex = "\"(.*)\" by (.*)".toRegex()
        val matchResult = regex.find(input.replace(Regex("\\(.*?\\)"), "").trim())
        val songTitle = matchResult?.groups?.get(1)?.value
        val artistName = matchResult?.groups?.get(2)?.value

        val artistNameParts = artistName?.split(" ") ?: emptyList()
        val formattedArtistName = if (artistNameParts.size >= 2) {
            val firstName = artistNameParts[0]
            val lastName = artistNameParts[1]
            "$lastName $firstName"
        } else {
            artistName
        }
        return "$formattedArtistName - $songTitle"
    }
}
