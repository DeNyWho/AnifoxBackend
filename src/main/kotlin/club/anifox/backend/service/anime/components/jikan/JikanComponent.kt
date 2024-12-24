package club.anifox.backend.service.anime.components.jikan

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.jikan.JikanDataDto
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDefaultDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDto
import club.anifox.backend.domain.dto.anime.jikan.character.JikanAnimeCharactersDto
import club.anifox.backend.domain.dto.anime.jikan.character.JikanCharacterDto
import club.anifox.backend.domain.dto.anime.jikan.character.JikanJpgCharacterDto
import club.anifox.backend.domain.dto.anime.jikan.image.JikanImagesDto
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

    suspend fun fetchJikanEpisodes(
        page: Int,
        shikimoriId: Int,
    ): JikanResponseDefaultDto<JikanEpisodeDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${shikimoriId}${Constants.JIKAN_EPISODES}"
            }
            parameter("page", page)
        }.body<JikanResponseDefaultDto<JikanEpisodeDto>>()
    }

    suspend fun fetchJikan(shikimoriId: Int): JikanResponseDto<JikanDataDto> {
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

    suspend fun fetchJikanAnimeCharacters(shikimoriId: Int): JikanResponseDefaultDto<JikanAnimeCharactersDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/$shikimoriId${Constants.JIKAN_CHARACTERS}"
            }
        }.body<JikanResponseDefaultDto<JikanAnimeCharactersDto>>()
    }

    suspend fun fetchJikanCharacter(characterId: Int): JikanResponseDto<JikanCharacterDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_CHARACTERS}/${characterId}${Constants.JIKAN_FULL}"
            }
        }.body<JikanResponseDto<JikanCharacterDto>>()
    }

    suspend fun fetchJikanCharacterPictures(characterId: Int): JikanResponseDefaultDto<JikanImagesDto<JikanJpgCharacterDto>> {
        return client.get {

            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_CHARACTERS}/${characterId}${Constants.JIKAN_PICTURES}"
            }
        }.body<JikanResponseDefaultDto<JikanImagesDto<JikanJpgCharacterDto>>>()
    }
}
