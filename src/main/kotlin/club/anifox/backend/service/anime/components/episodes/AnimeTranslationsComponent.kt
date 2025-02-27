package club.anifox.backend.service.anime.components.episodes

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import club.anifox.backend.domain.dto.anime.kodik.KodikTranslationsDto
import club.anifox.backend.domain.mappers.anime.toAnimeTranslation
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.util.anime.AnimeUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
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

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    @Value("\${anime.ko.token}")
    private lateinit var animeToken: String

    fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount> {
        val anime = animeUtils.checkAnime(url)

        val translationsCountEpisodes: Set<AnimeEpisodeTranslationCountTable> = anime.translationsCountEpisodes

        return translationsCountEpisodes
            .map { translationEpisodes ->
                AnimeTranslationCount(
                    translation = translationEpisodes.translation.toAnimeTranslation(),
                    countEpisodes = translationEpisodes.countEpisodes,
                )
            }
            .sortedByDescending { it.countEpisodes }
    }

    fun getAnimeTranslations(): List<AnimeTranslationTable> {
        return animeTranslationRepository.findAll()
    }

    fun addTranslationsToDB(translationsIDs: List<Int>) {
        val translations =
            runBlocking {
                client.get {
                    headers {
                        contentType(ContentType.Application.Json)
                    }
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.KODIK
                        encodedPath = "${Constants.KODIK_TRANSLATIONS}${Constants.KODIK_VERSION}"
                    }
                    parameter("token", animeToken)
                    parameter("types", "anime, anime-serial")
                }.body<KodikResponseDto<KodikTranslationsDto>>()
            }
        translationsIDs.forEach { translation ->
            val t = translations.result.find { it.id == translation }
            if (t != null) {
                checkKodikTranslation(t.id, t.title, "voice")
            }
        }
    }

    private fun checkKodikTranslation(
        translationId: Int,
        title: String,
        voice: String,
    ): AnimeTranslationTable {
        val translationCheck = animeTranslationRepository.findById(translationId).isPresent
        return if (translationCheck) {
            animeTranslationRepository.findById(translationId).get()
        } else {
            when (translationId) {
                1002 -> {
                    val studioCheck = animeTranslationRepository.findById(643).isPresent
                    if (studioCheck) {
                        animeTranslationRepository.findById(643).get()
                    } else {
                        AnimeTranslationTable(
                            id = 643,
                            title = "Studio Band",
                            voice = voice,
                        )
                    }
                }
                1272 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Anilibria",
                            voice = "sub",
                        ),
                    )
                }
                1291 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Crunchyroll",
                            voice = "sub",
                        ),
                    )
                }
                1946 -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = "Субтитры Netflix",
                            voice = "sub",
                        ),
                    )
                }
                else -> {
                    animeTranslationRepository.save(
                        AnimeTranslationTable(
                            id = translationId,
                            title = title,
                            voice = voice,
                        ),
                    )
                }
            }
        }
    }
}
