package club.anifox.backend.service.anime.components.kitsu

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.kitsu.KitsuAnimeDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuDefaultResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.episode.KitsuEpisodeDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KitsuComponent {

    @Autowired
    private lateinit var client: HttpClient

    suspend fun fetchKitsuEpisodes(url: String): KitsuDefaultResponseDto<KitsuEpisodeDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.KITSU
                encodedPath = url
            }
            header("Accept", "application/vnd.api+json")
        }.body<KitsuDefaultResponseDto<KitsuEpisodeDto>>()
    }

    suspend fun fetchKitsuAnime(kitsuId: Int): KitsuResponseDto<KitsuAnimeDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.KITSU
                encodedPath = "${Constants.KITSU_API}${Constants.KITSU_EDGE}${Constants.KITSU_ANIME}/$kitsuId"
            }
            header("Accept", "*/*")
        }.body<KitsuResponseDto<KitsuAnimeDto>>()
    }
}
