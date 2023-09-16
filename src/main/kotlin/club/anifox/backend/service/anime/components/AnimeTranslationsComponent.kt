package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import club.anifox.backend.domain.dto.anime.kodik.KodikTranslationsDto
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
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
class AnimeTranslationsComponent {

    @Autowired
    private lateinit var client: HttpClient

    @Autowired
    private lateinit var animeTranslationRepository: AnimeTranslationRepository

    @Value("\${anime.ko.token}")
    private lateinit var animeToken: String

    fun addTranslationsToDB(transltionsIDs: List<Int>) {
        val translations = runBlocking {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "kodikapi.com/translations/v2"
                }
                parameter("token", animeToken)
                parameter("types", "anime, anime-serial")
            }.body<KodikResponseDto<KodikTranslationsDto>>()
        }
        transltionsIDs.forEach { translation ->
            val t = translations.result.find { it.id == translation }
            if (t != null) {
                checkKodikTranslation(t.id, t.title, "voice")
            }
        }
    }

    private fun checkKodikTranslation(translationId: Int, title: String, voice: String): AnimeTranslationTable {
        val translationCheck = animeTranslationRepository.findById(translationId).isPresent
        return if(translationCheck) {
            animeTranslationRepository.findById(translationId).get()
        } else {
            when(translationId) {
                1002 -> {
                    val studioCheck = animeTranslationRepository.findById(643).isPresent
                    if(studioCheck) {
                        animeTranslationRepository.findById(643).get()
                    } else {
                        AnimeTranslationTable(
                            id = 643,
                            title = "Studio Band",
                            voice = voice
                        )
                    }
                }
                1272 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Anilibria",
                            voice = "sub"
                        )
                    )
                }
                1291 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Crunchyroll",
                            voice = "sub"
                        )
                    )
                }
                1946 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Netflix",
                            voice = "sub"
                        )
                    )
                }
                else -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = title,
                            voice = voice
                        )
                    )
                }
            }
        }
    }
}
