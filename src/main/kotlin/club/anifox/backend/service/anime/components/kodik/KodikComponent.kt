package club.anifox.backend.service.anime.components.kodik

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class KodikComponent {
    @Autowired
    private lateinit var client: HttpClient

    @Value("\${anime.ko.token}")
    private lateinit var animeToken: String

    suspend fun checkKodikSingle(
        shikimoriId: Int,
        translationID: String,
    ): KodikAnimeDto {
        return checkKodik(shikimoriId, translationID).result[0]
    }

    suspend fun checkKodikVariety(
        shikimoriId: Int,
        translationID: String,
    ): List<KodikAnimeDto> {
        return checkKodik(shikimoriId, translationID).result
    }

    suspend fun checkKodikList(translationsIds: String): KodikResponseDto<KodikAnimeDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.KODIK
                encodedPath = Constants.KODIK_LIST
            }
            parameter("token", animeToken)
            parameter("limit", 100)
            parameter("sort", "shikimori_rating")
            parameter("order", "desc")
            parameter("year", "${LocalDate.now().year}, ${LocalDate.now().year - 1}")
            parameter("types", "anime-serial, anime")
            parameter("camrip", false)
            parameter("with_episodes_data", true)
            parameter("not_blocked_in", "RU,UA")
            parameter("with_material_data", true)
            parameter("lgbt", false)
            parameter(
                "anime_genres",
                "безумие, боевые искусства, вампиры, военное, гарем, демоны," +
                    "детектив, детское, дзёсей, драма, игры, исторический, комедия," +
                    "космос, машины, меха, музыка, пародия, повседневность, полиция," +
                    "приключения, психологическое, романтика, самураи, сверхъестественное," +
                    "спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер," +
                    "ужасы, фантастика, фэнтези, школа, экшен",
            )
            parameter("translation_id", translationsIds)
        }.body<KodikResponseDto<KodikAnimeDto>>()
    }

    private suspend fun checkKodik(
        shikimoriId: Int,
        translationID: String,
    ): KodikResponseDto<KodikAnimeDto> {
        return client.get {
            headers {
                contentType(ContentType.Application.Json)
            }
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.KODIK
                encodedPath = Constants.KODIK_SEARCH
            }
            parameter("token", animeToken)
            parameter("with_material_data", true)
            parameter("sort", "shikimori_rating")
            parameter("order", "desc")
            parameter("types", "anime-serial, anime")
            parameter("camrip", false)
            parameter("shikimori_id", shikimoriId)
            parameter("with_episodes_data", true)
            parameter("not_blocked_in", "RU,UA")
            parameter("with_material_data", true)
            parameter("lgbt", false)
            parameter(
                "anime_genres",
                "безумие, боевые искусства, вампиры, военное, гарем, демоны," +
                    "детектив, детское, дзёсей, драма, игры, исторический, комедия," +
                    "космос, машины, меха, музыка, пародия, повседневность, полиция," +
                    "приключения, психологическое, романтика, самураи, сверхъестественное," +
                    "спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер," +
                    "ужасы, фантастика, фэнтези, школа, экшен",
            )
            parameter("translation_id", translationID)
        }.body<KodikResponseDto<KodikAnimeDto>>()
    }
}
