package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriAnimeIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMangaIdDto
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.AnimeRelatedTable
import club.anifox.backend.jpa.entity.anime.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.AnimeVideoTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedByStudioRepository
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRelatedRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.jpa.repository.anime.AnimeVideoRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.haglund.HaglundComponent
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class AnimeParseComponent(
    private val client: HttpClient,
    private val kodikComponent: KodikComponent,
    private val fetchImageComponent: FetchImageComponent,
    private val commonParserComponent: CommonParserComponent,
    private val haglundComponent: HaglundComponent,
    private val shikimoriComponent: AnimeShikimoriComponent,
    private val episodesComponent: EpisodesComponent,
    private val animeBlockedRepository: AnimeBlockedRepository,
    private val animeBlockedByStudioRepository: AnimeBlockedByStudioRepository,
    private val animeGenreRepository: AnimeGenreRepository,
    private val animeRelatedRepository: AnimeRelatedRepository,
    private val animeStudiosRepository: AnimeStudiosRepository,
    private val animeTranslationRepository: AnimeTranslationRepository,
    private val animeErrorParserRepository: AnimeErrorParserRepository,
    private val animeRepository: AnimeRepository,
    private val animeVideoRepository: AnimeVideoRepository,
) {
    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика", "Юри", "юри")

    fun addDataToDB() {
        val translationsIds = animeTranslationRepository.findAll().map { it.id }.joinToString(", ")
        var ar = runBlocking {
            kodikComponent.checkKodikList(translationsIds)
        }
        while (ar.nextPage != null) {
            ar.result.distinctBy { it.shikimoriId }.forEach Loop@{ animeTemp ->
                runBlocking {
                    processData(animeTemp)
                }
            }
            ar = runBlocking {
                client.get(ar.nextPage!!) {
                    headers {
                        contentType(ContentType.Application.Json)
                    }
                }.body()
            }
        }
    }

    private suspend fun processData(animeKodik: KodikAnimeDto) {
        try {
            val shikimori = shikimoriComponent.fetchAnime(animeKodik.shikimoriId)

            if (shikimori != null) {
                val shikimoriRating = shikimori.score.toDoubleOrNull() ?: 0.0
                val userRatesStats = shikimori.usersRatesStats.sumOf { it.value }

                runBlocking {
                    if (!animeBlockedRepository.findById(shikimori.id).isPresent &&
                        (userRatesStats > 1500 || (userRatesStats > 500 && shikimori.status == "ongoing")) &&
                        shikimori.studios.none { studio ->
                            animeBlockedByStudioRepository.findById(studio.id).isPresent
                        } &&
                        !animeRepository.findByShikimoriId(shikimori.id).isPresent
                    ) {
                        val genres = shikimori.genres
                            .filter { it.russian !in inappropriateGenres }
                            .map { genre ->
                                animeGenreRepository.findByGenre(genre.russian)
                                    .orElseGet {
                                        val newGenre = AnimeGenreTable(name = genre.russian)
                                        animeGenreRepository.save(newGenre)
                                        newGenre
                                    }
                            }

                        val studios = shikimori.studios
                            .map { studio ->
                                animeStudiosRepository.findByStudio(studio.name)
                                    .orElseGet {
                                        val newStudio = AnimeStudioTable(name = studio.name)
                                        animeStudiosRepository.save(newStudio)
                                        newStudio
                                    }
                            }

                        val type = when (shikimori.kind) {
                            "movie" -> AnimeType.Movie
                            "tv" -> AnimeType.Tv
                            "ova" -> AnimeType.Ova
                            "ona" -> AnimeType.Ona
                            "special" -> AnimeType.Special
                            "music" -> AnimeType.Music
                            else -> AnimeType.Tv
                        }

                        val airedOn = LocalDate.parse(shikimori.airedOn)
                        val releasedOn = when {
                            shikimori.releasedOn != null -> {
                                LocalDate.parse(shikimori.releasedOn)
                            }
                            type == AnimeType.Movie -> {
                                LocalDate.parse(shikimori.airedOn)
                            }
                            else -> null
                        }

                        val animeIdsDeferred = async {
                            haglundComponent.fetchHaglundIds(shikimori.id)
                        }

                        var urlLinkPath = commonParserComponent.translit(if (!shikimori.russianLic.isNullOrEmpty() && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)

                        if (animeRepository.findByUrl(urlLinkPath).isPresent) {
                            urlLinkPath = "${commonParserComponent.translit(if (shikimori.russianLic != null && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)}-${airedOn.year}"
                        }

                        val relationShikimoriIdsDeferred = async {
                            shikimoriComponent.fetchRelated(shikimori.id).take(30)
                        }

                        val similarShikimoriIdsDeferred = async {
                            shikimoriComponent.fetchSimilar(shikimori.id).take(30)
                        }

                        val videosShikimoriDeferred = async {
                            shikimoriComponent.fetchVideos(shikimori.id)
                        }

                        val shikimoriScreenshotsDeferred = async {
                            shikimoriComponent.fetchScreenshots(shikimori.id)
                        }

                        val status = when (shikimori.status) {
                            "released" -> AnimeStatus.Released
                            "ongoing" -> AnimeStatus.Ongoing
                            else -> AnimeStatus.Ongoing
                        }

                        val season = when (airedOn.month.value) {
                            12, 1, 2 -> AnimeSeason.Winter
                            3, 4, 5 -> AnimeSeason.Spring
                            6, 7, 8 -> AnimeSeason.Summer
                            else -> AnimeSeason.Fall
                        }

                        val ratingMpa = when (shikimori.rating) {
                            "g" -> "G"
                            "pg" -> "PG"
                            "pg_13" -> "PG-13"
                            "r" -> "R"
                            "r_plus" -> "R+"
                            else -> ""
                        }

                        val minimalAge = when (shikimori.rating) {
                            "g" -> 0
                            "pg" -> 12
                            "pg_13" -> 16
                            "r" -> 18
                            "r_plus" -> 18
                            else -> 0
                        }

                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                            .withZone(ZoneId.of("Europe/Moscow"))

                        val nextEpisode = if (shikimori.nextEpisodeAt != null) LocalDateTime.parse(shikimori.nextEpisodeAt, formatter) else null

                        val otherTitles = animeKodik.materialData.otherTitles.toMutableList()
                        otherTitles.add(if (shikimori.russianLic != null && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)

                        val similar = similarShikimoriIdsDeferred.await().toMutableList()

                        val relations = animeRelatedRepository.saveAll(
                            relationShikimoriIdsDeferred.await().map { relation ->
                                val shikimoriId = when (val media = relation.anime ?: relation.manga) {
                                    is ShikimoriAnimeIdDto -> media.id
                                    is ShikimoriMangaIdDto -> media.id
                                    else -> throw IllegalArgumentException("Неизвестный тип медиа")
                                }
                                AnimeRelatedTable(
                                    type = relation.relationRussian.toString(),
                                    shikimoriId = shikimoriId,
                                    typeEn = relation.relation.toString(),
                                )
                            },
                        )

                        val animeIds = animeIdsDeferred.await()

                        val imagesDeferred = async {
                            fetchImageComponent.fetchAndSaveAnimeImages(shikimori.id, animeIds.kitsu, urlLinkPath)
                        }

                        val (images, bufferedLargeImage) = imagesDeferred.await() ?: return@runBlocking

                        val episodesReady = episodesComponent.fetchEpisodes(
                            shikimoriId = shikimori.id,
                            kitsuId = animeIds.kitsu.toString(),
                            type = type,
                            urlLinkPath = urlLinkPath,
                            defaultImage = images.medium,
                        )

                        val translationsCountReady = episodesComponent.translationsCount(episodesReady)
                        val translations = translationsCountReady.map { it.translation }

                        val videos = videosShikimoriDeferred.await()?.let { videosList ->
                            animeVideoRepository.saveAll(
                                videosList
                                    .filter { it.hosting == "youtube" && it.kind != "episode_preview" }
                                    .map { video ->
                                        AnimeVideoTable(
                                            url = video.url,
                                            imageUrl = video.imageUrl,
                                            playerUrl = video.playerUrl,
                                            name = video.name,
                                            type = when (video.kind) {
                                                "ed" -> AnimeVideoType.Ending
                                                "op" -> AnimeVideoType.Opening
                                                "pv" -> AnimeVideoType.Trailer
                                                else -> AnimeVideoType.Other
                                            },
                                        )
                                    },
                            )
                        } ?: emptyList()

                        val screenshots = shikimoriScreenshotsDeferred.await().toMutableList()
                        screenshots.map { screenshot ->
                            fetchImageComponent.saveImage(screenshot, CompressAnimeImageType.Screenshot, urlLinkPath, true)
                        }

                        val animeToSave = AnimeTable(
                            type = type,
                            url = urlLinkPath,
                            playerLink = animeKodik.link,
                            title = if (!shikimori.russianLic.isNullOrEmpty() && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian,
                            titleEn = shikimori.english.map { it.toString() }.toMutableList(),
                            titleJapan = shikimori.japanese.toMutableList(),
                            synonyms = shikimori.synonyms.toMutableList(),
                            titleOther = otherTitles,
                            similarAnime = similar,
                            ids = AnimeIdsTable(
                                aniDb = animeIds.aniDb,
                                aniList = animeIds.aniList,
                                animePlanet = animeIds.animePlanet,
                                aniSearch = animeIds.aniSearch,
                                imdb = animeIds.imdb,
                                kitsu = animeIds.kitsu,
                                liveChart = animeIds.liveChart,
                                notifyMoe = animeIds.notifyMoe,
                                thetvdb = animeIds.theMovieDb,
                                myAnimeList = animeIds.myAnimeList,
                            ),
                            year = airedOn.year,
                            nextEpisode = nextEpisode,
                            episodesCount = when {
                                shikimori.episodes == 0 -> null
                                shikimori.episodes < episodesReady.size -> episodesReady.size
                                else -> shikimori.episodes
                            },
                            episodesAired = episodesReady.size,
                            shikimoriId = shikimori.id,
                            createdAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
                            airedOn = airedOn,
                            releasedOn = releasedOn,
                            updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
                            status = status,
                            description = shikimori.description.replace(Regex("\\[\\/?[a-z]+.*?\\]"), ""),
                            franchise = shikimori.franchise,
                            images = AnimeImagesTable(
                                large = images.large,
                                medium = images.medium,
                                cover = images.cover ?: "",
                            ),
                            screenshots = screenshots,
                            shikimoriRating = shikimoriRating,
                            shikimoriVotes = userRatesStats,
                            ratingMpa = ratingMpa,
                            minimalAge = minimalAge,
                            season = season,
                            accentColor = commonParserComponent.getMostCommonColor(bufferedLargeImage),
                        )

                        animeToSave.addTranslationCount(translationsCountReady)
                        animeToSave.addRelated(relations)
                        animeToSave.addTranslation(translations)
                        animeToSave.addEpisodesAll(episodesReady)
                        animeToSave.addAllAnimeGenre(genres)
                        animeToSave.addAllAnimeStudios(studios)
                        animeToSave.addVideos(videos)

                        val preparationToSaveAnime = animeRepository.findByShikimoriId(shikimori.id)
                        if (preparationToSaveAnime.isPresent) {
                            return@runBlocking
                        } else {
                            animeRepository.saveAndFlush(animeToSave)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handleException(e, animeKodik.shikimoriId)
        }
    }

    private fun handleException(e: Exception, shikimoriId: Int) {
        e.printStackTrace()
        animeErrorParserRepository.save(
            AnimeErrorParserTable(
                message = e.message,
                cause = "parser",
                shikimoriId = shikimoriId,
            ),
        )
    }
}
