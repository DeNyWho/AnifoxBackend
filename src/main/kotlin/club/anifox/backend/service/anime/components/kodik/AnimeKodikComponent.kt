package club.anifox.backend.service.anime.components.kodik

import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


@Component
class AnimeKodikComponent {

    @Autowired
    private lateinit var client: HttpClient

    @Value("\${anime.ko.token}")
    private lateinit var animeToken: String

    fun checkKodikSingle(shikimoriId: Int, translationID: String): KodikAnimeDto {
        return runBlocking {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "kodikapi.com/search"
                }
                parameter("token", animeToken)
                parameter("with_material_data", true)
                parameter("sort", "shikimori_rating")
                parameter("order", "desc")
                parameter("types", "anime-serial, anime")
                parameter("camrip", false)
                parameter("shikimori_id", shikimoriId)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
                parameter(
                    "anime_genres",
                    "безумие, боевые искусства, вампиры, военное, гарем, демоны, детектив, детское, дзёсей, драма, игры, исторический, комедия, космос, машины, меха, музыка, пародия, повседневность, полиция, приключения, психологическое, романтика, самураи, сверхъестественное, спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер, ужасы, фантастика, фэнтези, школа, экшен"
                )
                parameter("translation_id", translationID)
            }.body<KodikResponseDto<KodikAnimeDto>>()
        }.result[0]
    }

}
