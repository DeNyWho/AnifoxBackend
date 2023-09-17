package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.haglund.HaglundIdsDto
import club.anifox.backend.domain.dto.anime.jikan.JikanDataDto
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDefaultDto
import club.anifox.backend.domain.dto.anime.jikan.JikanResponseDto
import club.anifox.backend.domain.dto.anime.jikan.JikanThemesDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuAnimeDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuDefaultResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.episode.KitsuEpisodeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikEpisodeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriAnimeIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMangaIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriRelationDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriScreenshotsDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriSimilarDto
import club.anifox.backend.domain.dto.translate.edge.TranslateTextDto
import club.anifox.backend.domain.dto.translate.edge.TranslatedTextDto
import club.anifox.backend.domain.enums.anime.AnimeMusicType
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeBufferedImages
import club.anifox.backend.domain.model.anime.AnimeImagesTypes
import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.AnimeMediaTable
import club.anifox.backend.jpa.entity.anime.AnimeMusicTable
import club.anifox.backend.jpa.entity.anime.AnimeRelatedTable
import club.anifox.backend.jpa.entity.anime.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.EpisodeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeEpisodeTranslationRepository
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeMusicRepository
import club.anifox.backend.jpa.repository.anime.AnimeRelatedRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationCountRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.service.anime.components.kodik.AnimeKodikComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import club.anifox.backend.service.image.ImageService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

@Component
class AnimeParseComponent {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var client: HttpClient

    @Value("\${anime.ko.token}")
    private lateinit var animeToken: String

    @Autowired
    private lateinit var kodikComponent: AnimeKodikComponent

    @Autowired
    private lateinit var shikimoriComponent: AnimeShikimoriComponent

    @Autowired
    private lateinit var animeBlockedRepository: AnimeBlockedRepository

    @Autowired
    private lateinit var animeGenreRepository: AnimeGenreRepository

    @Autowired
    private lateinit var animeRelatedRepository: AnimeRelatedRepository

    @Autowired
    private lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    private lateinit var animeTranslationRepository: AnimeTranslationRepository

    @Autowired
    private lateinit var animeTranslationCountRepository: AnimeTranslationCountRepository

    @Autowired
    private lateinit var animeEpisodeTranslationRepository: AnimeEpisodeTranslationRepository

    @Autowired
    private lateinit var animeErrorParserRepository: AnimeErrorParserRepository

    @Autowired
    private lateinit var animeMusicRepository: AnimeMusicRepository

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var imageService: ImageService

    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика")

    fun addDataToDB(translationID: String) {
        var nextPage: String? = "1"
        var ar = runBlocking {
            client.get {
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
                parameter("types", "anime-serial, anime")
                parameter("camrip", false)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
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

        while (nextPage != null) {
            ar.result.distinctBy { it.shikimoriId }.forEach Loop@{ animeTemp ->
                try {
                    val anime = kodikComponent.checkKodikSingle(animeTemp.shikimoriId.toInt(), translationID)

                    val shikimori = shikimoriComponent.checkShikimori(anime.shikimoriId)

                    var userRatesStats = 0

                    shikimori?.usersRatesStats?.forEach {
                        userRatesStats += it.value
                    }

                    if (
                        !anime.materialData.title.contains("Атака Титанов") &&
                        !anime.materialData.title.contains("Атака титанов") &&
                        !animeBlockedRepository.findById(animeTemp.shikimoriId.toInt()).isPresent && anime.materialData.shikimoriVotes > 90 && userRatesStats > 1000 && shikimori != null &&
                        !anime.materialData.animeStudios.contains("Haoliners Animation League")
                    ) {
                        println(anime.shikimoriId.toInt())
                        val tempingAnime = animeRepository.findByShikimoriId(anime.shikimoriId.toInt())

                        if (!tempingAnime.isPresent) {
                            val genres = anime.materialData.genres
                                .filter { it !in inappropriateGenres }
                                .map { genre ->
                                    animeGenreRepository.findByGenre(genre)
                                        .orElseGet {
                                            val newGenre = AnimeGenreTable(name = genre)
                                            animeGenreRepository.save(newGenre)
                                            newGenre
                                        }
                                }

                            val studios = anime.materialData.animeStudios
                                .map { studio ->
                                    animeStudiosRepository.findByStudio(studio)
                                        .orElseGet {
                                            val newStudio = AnimeStudioTable(name = studio)
                                            animeStudiosRepository.save(newStudio)
                                            newStudio
                                        }
                                }

                            val animeIds = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = Constants.HAGLUND
                                        encodedPath = "${Constants.HAGLUND_API}${Constants.HAGLUND_VERSION}${Constants.HAGLUND_IDS}"
                                    }
                                    parameter("source", "myanimelist")
                                    parameter("id", anime.shikimoriId)
                                }.body<HaglundIdsDto>()
                            }

                            val titleRussianLic = shikimori.russianLic
                            val titleRussian = shikimori.russian

                            var urlLinking = translit(if (titleRussianLic != null && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian)

                            if (animeRepository.findByUrl(urlLinking).isPresent) {
                                urlLinking = "${translit(if (titleRussianLic != null && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian)}-${if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.year}"
                            }

                            val relationIdsDeferred = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = Constants.SHIKIMORI
                                            encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${anime.shikimoriId}${Constants.SHIKIMORI_RELATED}"
                                        }
                                    }.body<List<ShikimoriRelationDto>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            val relationIds = mutableListOf<ShikimoriRelationDto>()

                            runBlocking {
                                val temp = relationIdsDeferred.await()
                                if (temp != null) {
                                    relationIds.addAll(temp)
                                }
                            }

                            val relations = animeRelatedRepository.saveAll(
                                relationIds.take(30).map { relation ->
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
                            ).toMutableList()

                            val similarIdsFlow = flow {
                                delay(1000)
                                val similarIds = client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = Constants.SHIKIMORI
                                        encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${anime.shikimoriId}${Constants.SHIKIMORI_SIMILAR}"
                                    }
                                }.body<List<ShikimoriSimilarDto>>().flatMap { similar ->
                                    listOfNotNull(similar.id)
                                }.map { it }

                                emit(similarIds.take(30))
                            }.flowOn(Dispatchers.IO)

                            val media = shikimori.videos
                                .filter { it.hosting != "vk" }
                                .map { video ->
                                    AnimeMediaTable(
                                        url = video.url,
                                        imageUrl = video.imageUrl,
                                        playerUrl = video.playerUrl,
                                        name = video.name,
                                        kind = video.kind,
                                        hosting = video.hosting,
                                    )
                                }

                            var animeImages: AnimeImagesTypes? = null

                            var image: AnimeBufferedImages? = null

                            val kitsuAnime = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    client.get {
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = Constants.KITSU
                                            encodedPath = "${Constants.KITSU_API}${Constants.KITSU_EDGE}${Constants.KITSU_ANIME}/${animeIds.kitsu}"
                                        }
                                        header("Accept", "application/vnd.api+json")
                                    }.body<KitsuResponseDto<KitsuAnimeDto>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            val jikanImage = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        headers {
                                            contentType(ContentType.Application.Json)
                                        }
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = Constants.JIKAN
                                            encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${anime.shikimoriId}"
                                        }
                                    }.body<JikanResponseDto<JikanDataDto>>()
                                }.getOrElse {
                                    null
                                }
                            }
                            var imagesCallback: ((AnimeImagesTypes?, AnimeBufferedImages?) -> Unit)? = null

                            fun initImages(callback: (AnimeImagesTypes?, AnimeBufferedImages?) -> Unit) {
                                imagesCallback = callback

                                runBlocking {
                                    val kitsuData = kitsuAnime.await()?.data
                                    val jikanData = jikanImage.await()?.data

                                    try {
                                        when {
                                            kitsuData != null -> {
                                                animeImages = AnimeImagesTypes(
                                                    large = imageService.saveFileInSThird(
                                                        "images/large/$urlLinking.png",
                                                        URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                                        compress = true,
                                                        width = 400,
                                                        height = 640,
                                                    ),
                                                    medium = imageService.saveFileInSThird(
                                                        "images/medium/$urlLinking.png",
                                                        URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                                        compress = true,
                                                        width = 200,
                                                        height = 440,
                                                    ),
                                                    cover = try {
                                                        if (kitsuData.attributesKitsu.coverImage.coverLarge != null) {
                                                            imageService.saveFileInSThird(
                                                                "images/cover/$urlLinking.png",
                                                                URL(kitsuData.attributesKitsu.coverImage.coverLarge).readBytes(),
                                                                compress = true,
                                                                width = 800,
                                                                height = 200,
                                                            )
                                                        } else {
                                                            null
                                                        }
                                                    } catch (e: Exception) {
                                                        null
                                                    },
                                                )
                                                image = AnimeBufferedImages(
                                                    large = ImageIO.read(URL(kitsuData.attributesKitsu.posterImage.original)),
                                                    medium = ImageIO.read(URL(kitsuData.attributesKitsu.posterImage.large)),
                                                )
                                            }
                                            jikanData != null -> {
                                                animeImages = AnimeImagesTypes(
                                                    large = imageService.saveFileInSThird(
                                                        "images/large/$urlLinking.png",
                                                        URL(jikanData.images.jikanJpg.largeImageUrl).readBytes(),
                                                    ),
                                                    medium = imageService.saveFileInSThird(
                                                        "images/medium/$urlLinking.png",
                                                        URL(jikanData.images.jikanJpg.mediumImageUrl).readBytes(),
                                                    ),
                                                )
                                                image = AnimeBufferedImages(
                                                    large = ImageIO.read(URL(jikanData.images.jikanJpg.largeImageUrl)),
                                                    medium = ImageIO.read(URL(jikanData.images.jikanJpg.mediumImageUrl)),
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (jikanData != null) {
                                            animeImages = AnimeImagesTypes(
                                                large = imageService.saveFileInSThird(
                                                    "images/large/$urlLinking.png",
                                                    URL(jikanData.images.jikanJpg.largeImageUrl).readBytes(),
                                                ),
                                                medium = imageService.saveFileInSThird(
                                                    "images/medium/$urlLinking.png",
                                                    URL(jikanData.images.jikanJpg.mediumImageUrl).readBytes(),
                                                ),
                                            )
                                            image = AnimeBufferedImages(
                                                large = ImageIO.read(URL(jikanData.images.jikanJpg.largeImageUrl)),
                                                medium = ImageIO.read(URL(jikanData.images.jikanJpg.mediumImageUrl)),
                                            )
                                        } else {
                                            return@runBlocking
                                        }
                                    }
                                    // Вызываем callback с результатами
                                    imagesCallback?.invoke(animeImages, image)
                                }
                            }

                            var aI: AnimeImagesTypes? = null
                            var aB: AnimeBufferedImages? = null

                            initImages { animeImages, image ->
                                aI = animeImages
                                aB = image
                            }

                            val episodesReady = mutableListOf<AnimeEpisodeTable>()

                            when (anime.type) {
                                "anime-serial" -> {
                                    anime.seasons.forEach { kodikSeason ->
                                        if (kodikSeason.key != "0") {
                                            val jikanEpisodes = mutableListOf<JikanEpisodeDto>()
                                            val kitsuEpisodes = mutableListOf<KitsuEpisodeDto>()

                                            // Run kitsu.io and jikan.moe API calls in parallel using async and await
                                            runBlocking {
                                                val deferredKitsu = async {
                                                    val kitsuAsyncTask = async { fetchKitsuEpisodes("api${Constants.KITSU_EDGE}${Constants.KITSU_ANIME}/${animeIds.kitsu}${Constants.KITSU_EPISODES}") }

                                                    var responseKitsuEpisodes = kitsuAsyncTask.await()
                                                    while (responseKitsuEpisodes.data != null) {
                                                        kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
                                                        val kitsuUrl = responseKitsuEpisodes.links.next?.replace("https://${Constants.KITSU}", "").toString()
                                                        responseKitsuEpisodes = if (kitsuUrl != "null") fetchKitsuEpisodes(kitsuUrl) else KitsuDefaultResponseDto()
                                                    }
                                                }
                                                val deferredJikan = async {
                                                    val jikanAsyncTask = async { fetchJikanEpisodes(1, anime.shikimoriId) }

                                                    var responseJikanEpisodes = jikanAsyncTask.await()
                                                    var page = 1
                                                    jikanEpisodes.addAll(responseJikanEpisodes.data)
                                                    while (responseJikanEpisodes.data.isNotEmpty()) {
                                                        page++
                                                        responseJikanEpisodes = fetchJikanEpisodes(page, anime.shikimoriId)
                                                        jikanEpisodes.addAll(responseJikanEpisodes.data)
                                                    }
                                                }
                                                deferredJikan.await()
                                                deferredKitsu.await()
                                            }

                                            episodesReady.addAll(
                                                runBlocking {
                                                    processEpisodes(
                                                        type = "anime-serial",
                                                        anime.shikimoriId,
                                                        urlLinking,
                                                        kodikSeason.value.episodes,
                                                        kitsuEpisodes,
                                                        jikanEpisodes,
                                                        animeImages!!.medium,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }

                                "anime" -> {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        val episode = processEpisode(
                                            type = "anime",
                                            anime.shikimoriId,
                                            anime.link,
                                            1,
                                            null,
                                            null,
                                            null,
                                            animeImages!!.medium,
                                            null,
                                        )
                                        episodesReady.addAll(addEpisodeTranslations(listOf(episode), anime.shikimoriId, "anime"))
                                    }
                                }
                            }

                            val jikanThemesDeferred = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        headers {
                                            contentType(ContentType.Application.Json)
                                        }
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = Constants.JIKAN
                                            encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${anime.shikimoriId}${Constants.JIKAN_THEMES}"
                                        }
                                    }.body<JikanResponseDto<JikanThemesDto>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            val music: MutableList<AnimeMusicTable> = mutableListOf()

                            CoroutineScope(Dispatchers.Default).launch {
                                val jikanData = jikanThemesDeferred.await()?.data
                                val musicToSave = mutableListOf<AnimeMusicTable>()

                                if (jikanData != null) {
                                    jikanData.endings.forEach { ending ->
                                        if (ending != null) {
                                            val endingNormalize = jikanThemesNormalize(ending)
                                            musicToSave.add(
                                                AnimeMusicTable(
                                                    url = "https://${Constants.YOUTUBE_MUSIC}${Constants.YOUTUBE_SEARCH}$endingNormalize",
                                                    name = endingNormalize,
                                                    episodes = mergeEpisodes(ending),
                                                    type = AnimeMusicType.Ending,
                                                    hosting = "YoutubeMusic",
                                                ),
                                            )
                                        }
                                    }

                                    jikanData.openings.forEach { opening ->
                                        if (opening != null) {
                                            val openingNormalize = jikanThemesNormalize(opening)
                                            musicToSave.add(
                                                AnimeMusicTable(
                                                    url = "https://${Constants.YOUTUBE_MUSIC}${Constants.YOUTUBE_SEARCH}$openingNormalize",
                                                    name = openingNormalize,
                                                    episodes = mergeEpisodes(opening),
                                                    type = AnimeMusicType.Opening,
                                                    hosting = "YoutubeMusic",
                                                ),
                                            )
                                        }
                                    }

                                    animeMusicRepository.saveAll(musicToSave)
                                }
                            }

                            val screenshotsFlow = flow {
                                delay(1500)
                                val screenshots = client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = Constants.SHIKIMORI
                                        encodedPath = "${Constants.SHIKIMORI_API}${Constants.SHIKIMORI_ANIMES}/${anime.shikimoriId}${Constants.SHIKIMORI_SCREENSHOTS}"
                                    }
                                }.body<List<ShikimoriScreenshotsDto>>().map { screenshot ->
                                    "https://${Constants.SHIKIMORI}${screenshot.original}"
                                }

                                emit(screenshots)
                            }.flowOn(Dispatchers.IO)

                            val screenShots = mutableListOf<String>()
                            val similarIds = mutableListOf<Int>()

                            runBlocking {
                                screenshotsFlow.collect {
                                    screenShots.addAll(it)
                                }
                                similarIdsFlow.collect {
                                    similarIds.addAll(it)
                                }
                            }

                            val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                .withZone(ZoneId.of("Europe/Moscow"))

                            val otherTitles = anime.materialData.otherTitles.toMutableList()

                            otherTitles.add(if (titleRussianLic != null && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian)

                            val translationsAll = animeTranslationRepository.findAll()

                            val translationsCountMap = episodesReady
                                .flatMap { it.translations }
                                .groupBy { it.translation.id }
                                .map { (id, translations) ->
                                    AnimeEpisodeTranslationCountTable(
                                        translation = translationsAll.find { it.id == id }!!,
                                        countEpisodes = translations.size,
                                    )
                                }

                            val translationsCountReady = animeTranslationCountRepository.saveAll(translationsCountMap)

                            val translations = translationsCountReady.map { it.translation }

                            val status = when (shikimori.status) {
                                "released" -> AnimeStatus.Released
                                "ongoing" -> AnimeStatus.Ongoing
                                else -> AnimeStatus.Ongoing
                            }

                            val season = when (anime.materialData.airedAt.month.value) {
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

                            val type = when (anime.materialData.animeType) {
                                "movie" -> AnimeType.Movie
                                "tv" -> AnimeType.Tv
                                "ova" -> AnimeType.Ova
                                "ona" -> AnimeType.Ona
                                "special" -> AnimeType.Special
                                "music" -> AnimeType.Music
                                else -> AnimeType.Tv
                            }

                            val airedOn = shikimori.airedAt?.let { LocalDate.parse(it) }
                                ?: if (shikimori.episodes == 1) {
                                    shikimori.releasedAt?.let { LocalDate.parse(it) }
                                        ?: anime.materialData.releasedAt
                                } else {
                                    anime.materialData.airedAt
                                }

                            val a = AnimeTable(
                                title = if (titleRussianLic != null && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian,
                                url = urlLinking,
                                ids = AnimeIdsTable(
                                    aniDb = animeIds.aniDb,
                                    aniList = animeIds.aniList,
                                    animePlanet = animeIds.animePlanet,
                                    imdb = animeIds.imdb,
                                    kitsu = animeIds.kitsu,
                                    liveChart = animeIds.liveChart,
                                    notifyMoe = animeIds.notifyMoe,
                                    thetvdb = animeIds.theMovieDb,
                                    myAnimeList = animeIds.myAnimeList,
                                ),
                                nextEpisode = if (shikimori.nextEpisodeAt != null) {
                                    LocalDateTime.parse(shikimori.nextEpisodeAt, formatterUpdated)
                                } else {
                                    null
                                },
                                images = AnimeImagesTable(
                                    large = animeImages?.large ?: "",
                                    medium = animeImages?.medium ?: "",
                                    cover = animeImages?.cover ?: "",
                                ),
                                titleEn = shikimori.english.toMutableList(),
                                titleJapan = shikimori.japanese.toMutableList(),
                                synonyms = shikimori.synonyms.toMutableList(),
                                titleOther = otherTitles,
                                similarAnime = similarIds,
                                status = status,
                                description = shikimori.description.replace(Regex("\\[\\/?[a-z]+.*?\\]"), ""),
                                year = if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.airedAt.year,
                                createdAt = anime.createdAt,
                                playerLink = anime.link,
                                airedOn = airedOn,
                                releasedOn = if (shikimori.releasedAt != null) LocalDate.parse(shikimori.releasedAt) else anime.materialData.releasedAt,
                                episodesCount = shikimori.episodes,
                                episodesAired = if (shikimori.status == "released") shikimori.episodes else episodesReady.size,
                                type = type,
                                updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
                                minimalAge = minimalAge,
                                screenshots = screenShots,
                                ratingMpa = ratingMpa,
                                shikimoriId = anime.shikimoriId.toInt(),
                                shikimoriRating = anime.materialData.shikimoriRating,
                                shikimoriVotes = anime.materialData.shikimoriVotes,
                                season = season,
                                accentColor = getMostCommonColor(image?.large!!),
                            )
                            a.addTranslationCount(translationsCountReady)
                            a.addRelated(relations)
                            a.addTranslation(translations)
                            a.addEpisodesAll(episodesReady)
                            a.addAllMusic(music)
                            a.addAllAnimeGenre(genres)
                            a.addAllAnimeStudios(studios)
                            a.addMediaAll(media)

                            val preparationToSaveAnime = animeRepository.findByShikimoriId(a.shikimoriId)
                            if (preparationToSaveAnime.isPresent) {
                                return@Loop
                            } else {
                                animeRepository.saveAndFlush(a)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.stackTrace.forEach {
                        println(it)
                    }
                    animeErrorParserRepository.save(
                        AnimeErrorParserTable(
                            message = e.message,
                            cause = "ANIME PARSE",
                            shikimoriId = animeTemp.shikimoriId.toInt(),
                        ),
                    )
                    return@Loop
                }
            }
            if (ar.nextPage != null) {
                ar = runBlocking {
                    client.get(ar.nextPage!!) {
                        headers {
                            contentType(ContentType.Application.Json)
                        }
                    }.body()
                }
            }
            nextPage = ar.nextPage
        }
    }

    private suspend fun processEpisodes(
        type: String,
        shikimoriId: String,
        urlLinking: String,
        kodikEpisodes: Map<String, KodikEpisodeDto>,
        kitsuEpisodes: List<KitsuEpisodeDto>,
        jikanEpisodes: List<JikanEpisodeDto>,
        imageDefault: String,
    ): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()

        val kitsuEpisodesMapped = mutableMapOf<String, KitsuEpisodeDto?>()
        val translatedTitleMapped = mutableMapOf<String, String>()
        val translatedDescriptionMapped = mutableMapOf<String, String>()
        val tempTranslatedTitle = mutableListOf<TranslateTextDto>()
        val tempTranslatedDescription = mutableListOf<TranslateTextDto>()

        if (jikanEpisodes.size >= kodikEpisodes.size) {
            jikanEpisodes.map { episode ->
                val number = episode.id
                val kitsuEpisode = findEpisodeByNumber(number, kitsuEpisodes)
                kitsuEpisodesMapped[number.toString()] = kitsuEpisode
                translatedTitleMapped[number.toString()] = jikanEpisodes[number - 1].title
                translatedDescriptionMapped[number.toString()] = kitsuEpisode?.attributes?.description ?: ""
            }
        } else {
            kodikEpisodes.map { (episodeKey, episode) ->
                if (episodeKey.toInt() <= kitsuEpisodes.size) {
                    val kitsuEpisode = findEpisodeByNumber(episodeKey.toInt(), kitsuEpisodes)
                    kitsuEpisodesMapped[episodeKey] = kitsuEpisode
                    translatedDescriptionMapped[episodeKey] = kitsuEpisode?.attributes?.description ?: ""
                }
                if (episodeKey.toInt() <= jikanEpisodes.size) {
                    translatedTitleMapped[episodeKey] = when (episodeKey) {
                        "0" -> {
                            if (kodikEpisodes["0"] != null && jikanEpisodes[episodeKey.toInt()].id != 0) {
                                episodeKey
                            } else {
                                jikanEpisodes[episodeKey.toInt()].title
                            }
                        }
                        "1" -> {
                            jikanEpisodes[episodeKey.toInt() - 1].title
                        }
                        else -> {
                            jikanEpisodes[episodeKey.toInt() - 1].title
                        }
                    }
                }
            }
        }

        translatedTitleMapped.map { (episodeKey, title) ->
            tempTranslatedTitle.add(TranslateTextDto(title))
        }

        translatedDescriptionMapped.map { (episodeKey, description) ->
            tempTranslatedDescription.add(TranslateTextDto(description))
        }

        val a = if (tempTranslatedTitle.size < 61) {
            translateText(tempTranslatedTitle)
        } else {
            val tempList = mutableListOf<String>()
            val temp = tempTranslatedTitle.chunked(60)
            temp.forEach {
                tempList.addAll(translateText(it))
            }
            tempList
        }

        val b = if (tempTranslatedDescription.size < 61) {
            translateText(tempTranslatedDescription)
        } else {
            val tempList = mutableListOf<String>()
            val temp = tempTranslatedDescription.chunked(60)
            temp.forEach {
                tempList.addAll(translateText(it))
            }
            tempList
        }

        translatedTitleMapped.map { (episodeKey, title) ->
            val episodeKeyList = when (episodeKey) {
                "0" -> {
                    episodeKey.toInt()
                }
                "1" -> {
                    if (translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
                }
                else -> {
                    if (translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
                }
            }
            translatedTitleMapped[episodeKey] = a[episodeKeyList]
        }

        translatedDescriptionMapped.map { (episodeKey, title) ->
            val number = if (translatedDescriptionMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
            translatedDescriptionMapped[episodeKey] = b[number]
        }

        val jobs = kodikEpisodes.map { (episodeKey, episode) ->
            CoroutineScope(Dispatchers.Default).async {
                processEpisode(
                    type,
                    shikimoriId,
                    urlLinking,
                    episodeKey.toInt(),
                    kitsuEpisodesMapped[episodeKey],
                    translatedTitleMapped[episodeKey],
                    translatedDescriptionMapped[episodeKey],
                    imageDefault,
                    try {
                        if (kodikEpisodes["0"] != null && jikanEpisodes[episodeKey.toInt() - 1].id != 0) null else jikanEpisodes[episodeKey.toInt() - 1]
                    } catch (e: Exception) {
                        null
                    },
                )
            }
        }

        val processedEpisodes = jobs.awaitAll()
        val sortedEpisodes = processedEpisodes.sortedBy { it.number }

        episodeReady.addAll(addEpisodeTranslations(sortedEpisodes, shikimoriId, "anime-serial"))

        return episodeReady
    }

    private suspend fun processEpisode(
        type: String,
        shikimoriId: String,
        url: String,
        episode: Int,
        kitsuEpisode: KitsuEpisodeDto?,
        titleRu: String?,
        descriptionRu: String?,
        imageDefault: String,
        jikanEpisode: JikanEpisodeDto?,
    ): AnimeEpisodeTable {
        println("EPISODE EPISODE EPISODE")
        return if (kitsuEpisode != null) {
            val imageEpisode = try {
                when {
                    kitsuEpisode.attributes?.thumbnail?.large != null -> {
                        imageService.saveFileInSThird(
                            "images/episodes/$url/$episode.png",
                            URL(kitsuEpisode.attributes.thumbnail.large).readBytes(),
                            compress = false,
                        )
                    }
                    kitsuEpisode.attributes?.thumbnail?.original != null -> {
                        imageService.saveFileInSThird(
                            "images/episodes/$url/$episode.png",
                            URL(kitsuEpisode.attributes.thumbnail.original).readBytes(),
                            compress = true,
                            width = 400,
                            height = 225,
                        )
                    }
                    else -> imageDefault
                }
            } catch (e: Exception) {
                imageDefault
            }

            val kitsuNumber = kitsuEpisode.attributes?.number ?: episode

            val airedDate = when {
                jikanEpisode != null && jikanEpisode.aired.length > 3 -> {
                    jikanEpisode.aired
                }
                kitsuEpisode.attributes?.airDate != null && kitsuEpisode.attributes.airDate.length > 3 -> {
                    kitsuEpisode.attributes.airDate
                }
                else -> null
            }

            return AnimeEpisodeTable(
                title = if (titleRu != null && titleRu.length > 3) titleRu else "$episode",
                titleEn = kitsuEpisode.attributes?.titles?.enToUs ?: "",
                description = descriptionRu,
                descriptionEn = kitsuEpisode.attributes?.description ?: "",
                number = kitsuNumber,
                image = if (imageEpisode.length > 5) imageEpisode else imageDefault,
                filler = jikanEpisode?.filler ?: false,
                recap = jikanEpisode?.recap ?: false,
                aired = if (airedDate != null) LocalDate.parse(if (airedDate.length > 10) airedDate.substring(0, 10) else airedDate, DateTimeFormatter.ISO_DATE) else null,
            )
        } else {
            val airedDate = when {
                jikanEpisode != null && jikanEpisode.aired.length > 3 -> {
                    jikanEpisode.aired
                }
                else -> null
            }

            AnimeEpisodeTable(
                title = episode.toString(),
                titleEn = episode.toString(),
                description = null,
                descriptionEn = null,
                number = episode,
                image = imageDefault,
                filler = jikanEpisode?.filler ?: false,
                recap = jikanEpisode?.recap ?: false,
                aired = if (airedDate != null) LocalDate.parse(if (airedDate.length > 10) airedDate.substring(0, 10) else airedDate, DateTimeFormatter.ISO_DATE) else null,
            )
        }
    }

    private fun addEpisodeTranslations(episodes: List<AnimeEpisodeTable>, shikimoriId: String, type: String): List<AnimeEpisodeTable> {
        val animeVariations = runBlocking {
            client.get {
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
                parameter("types", type)
                parameter("camrip", false)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
                parameter("shikimori_id", shikimoriId)
                parameter(
                    "anime_genres",
                    "безумие, боевые искусства, вампиры, военное, гарем, демоны," +
                        "детектив, детское, дзёсей, драма, игры, исторический, комедия," +
                        "космос, машины, меха, музыка, пародия, повседневность, полиция," +
                        "приключения, психологическое, романтика, самураи, сверхъестественное," +
                        "спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер," +
                        "ужасы, фантастика, фэнтези, школа, экшен",
                )
                parameter("translation_id", "610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002, 1978, 1291, 1272, 1946")
            }.body<KodikResponseDto<KodikAnimeDto>>()
        }

        val episodeTranslationsToSave = mutableListOf<EpisodeTranslationTable>()

        animeVariations.result.forEach { anime ->
            val episodeNumbers = anime.seasons.values
                .flatMap { it.episodes.keys.mapNotNull { key -> key.toIntOrNull() } }
//                .filter { it <= anime.lastEpisode || anime.lastEpisode == 0 }

            val translationId = when (anime.translation.id) {
                1002 -> 643
                else -> anime.translation.id
            }
            val translation = animeTranslationRepository.findById(translationId).get()

            episodeNumbers.forEach { episodeNumber ->
                val episode = episodes.find { it.number == episodeNumber }
                episode?.let {
                    val episodeTranslation = EpisodeTranslationTable(
                        translation = translation,
                        link = if (type == "anime-serial") "${anime.link}?episode=${episode.number}" else anime.link,
                    )
                    episode.translations.add(episodeTranslation)
                    episodeTranslationsToSave.add(episodeTranslation)
                }
            }
        }

        animeEpisodeTranslationRepository.saveAll(episodeTranslationsToSave)
        return episodes
    }

    private fun findEpisodeByNumber(number: Int, kitsuEpisodes: List<KitsuEpisodeDto>): KitsuEpisodeDto? {
        return kitsuEpisodes.find { kitsuEpisode ->
            kitsuEpisode.attributes?.number == number
        }
    }

    private fun mergeEpisodes(input: String): String {
        try {
            val numbersRegex = Regex("(\\d+)-(\\d+)")
            val rangeMatches = numbersRegex.findAll(input)

            val extractedRanges = mutableListOf<String>()
            for (match in rangeMatches) {
                val start = match.groupValues[1]
                val end = match.groupValues[2]
                extractedRanges.add("$start-$end")
            }

            val distinctNumbers = extractedRanges.flatMap { range ->
                val (start, end) = range.split("-").map { it.toInt() }
                (start..end).toList()
            }.distinct()

            val numbersOnlyRegex = Regex("\\b\\d+\\b")
            val numberMatches = numbersOnlyRegex.findAll(input)

            val extractedNumbers = mutableListOf<String>()
            for (match in numberMatches) {
                val number = match.value
                extractedNumbers.add(number)
            }

            if (input.startsWith(extractedNumbers.first())) {
                extractedNumbers.removeAt(0)
            }

            val distinctSingleNumbers = extractedNumbers
                .distinct()
                .mapNotNull { it.toIntOrNull() }
                .filter { it !in distinctNumbers }

            return mergeNumbers(distinctNumbers + distinctSingleNumbers)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun mergeNumbers(numbers: List<Int>): String {
        if (numbers.isEmpty()) return ""

        val ranges = mutableListOf<Pair<Int, Int>>()
        var currentStart = numbers.first()

        for (number in numbers) {
            currentStart = if (number == currentStart || number == currentStart + 1) {
                number
            } else {
                ranges.add(currentStart to number - 1)
                number
            }
        }

        ranges.add(currentStart to numbers.last())

        return ranges.joinToString(", ") { (start, end) ->
            if (start == end) start.toString() else "$start-$end"
        }
    }

    private fun jikanThemesNormalize(input: String): String {
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

    private suspend fun translateText(text: List<TranslateTextDto>): List<String> {
        return if (text.isNotEmpty()) {
            val translatedText = try {
                client.post {
                    bearerAuth(
                        client.get {
                            url {
                                protocol = URLProtocol.HTTPS
                                host = Constants.EDGE
                                encodedPath = "${Constants.EDGE_TRANSLATE}${Constants.EDGE_AUTH}"
                            }
                            header("Accept", "application/vnd.api+json")
                        }.bodyAsText(),
                    )
                    url {
                        protocol = URLProtocol.HTTPS
                        host = Constants.MICROSOFT
                        encodedPath = Constants.MICROSOFT_TRANSLATE
                    }
                    setBody(text)
                    header("Accept", "application/vnd.api+json")
                    parameter("from", "en")
                    parameter("to", "ru")
                    parameter("api-version", "3.0")
                }.body<List<TranslatedTextDto>>()
            } catch (e: Exception) {
                try {
                    delay(1000)
                    client.post {
                        bearerAuth(
                            client.get {
                                url {
                                    protocol = URLProtocol.HTTPS
                                    host = Constants.EDGE
                                    encodedPath = "${Constants.EDGE_TRANSLATE}${Constants.EDGE_AUTH}"
                                }
                                header("Accept", "application/vnd.api+json")
                            }.bodyAsText(),
                        )
                        url {
                            protocol = URLProtocol.HTTPS
                            host = Constants.MICROSOFT
                            encodedPath = Constants.MICROSOFT_TRANSLATE
                        }
                        setBody(text)
                        header("Accept", "application/vnd.api+json")
                        parameter("from", "en")
                        parameter("to", "ru")
                        parameter("api-version", "3.0")
                    }.body<List<TranslatedTextDto>>()
                } catch (e: Exception) {
                    return listOf()
                }
            }
            val tempResult = mutableListOf<String>()

            translatedText.forEach {
                it.translations.forEach { translatedMicrosoft ->
                    tempResult.add(translatedMicrosoft.text ?: "")
                }
            }
            return tempResult
        } else {
            listOf()
        }
    }

    private suspend fun fetchKitsuEpisodes(url: String): KitsuDefaultResponseDto<KitsuEpisodeDto> {
        delay(1000)
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.KITSU
                encodedPath = url
            }
            header("Accept", "application/vnd.api+json")
        }.body<KitsuDefaultResponseDto<KitsuEpisodeDto>>()
    }

    private suspend fun fetchJikanEpisodes(page: Int, shikimoriId: String): JikanResponseDefaultDto<JikanEpisodeDto> {
        delay(1000)
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = Constants.JIKAN
                encodedPath = "${Constants.JIKAN_VERSION}${Constants.JIKAN_ANIME}/${shikimoriId}${Constants.JIKAN_EPISODES}"
            }
            parameter("page", page)
        }.body<JikanResponseDefaultDto<JikanEpisodeDto>>()
    }

    private fun getMostCommonColor(image: BufferedImage): String {
        val brightestColors = getBrightestColors(image)
        val numColors = brightestColors.size

        if (numColors == 0) {
            return "#FFFFFF"
        }

        val red = brightestColors.sumOf { it.red } / numColors
        val green = brightestColors.sumOf { it.green } / numColors
        val blue = brightestColors.sumOf { it.blue } / numColors

        return String.format("#%02X%02X%02X", red, green, blue)
    }

    private fun getBrightestColors(image: BufferedImage): List<Color> {
        val colorCount = mutableMapOf<Int, Int>()

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                val red = (pixel shr 16 and 0xFF) / 255.0
                val green = (pixel shr 8 and 0xFF) / 255.0
                val blue = (pixel and 0xFF) / 255.0

                if (red > 0.9 && green > 0.9 && blue > 0.9) {
                    continue // Белый цвет
                }
                if (red < 0.4 && green < 0.4 && blue < 0.4) {
                    continue // Черный цвет
                }
                if (red > 0.9 && green > 0.9 && blue > 0.8) {
                    continue // Очень светлый цвет
                }

                colorCount[pixel] = colorCount.getOrDefault(pixel, 0) + 1
            }
        }

        val mostCommonPixels = colorCount.filter { it.value > 30 }.keys

        return mostCommonPixels.map { Color(it) }.sortedByDescending { colorBrightness(it) }
    }

    private fun colorBrightness(color: Color): Double {
        return (color.red * 299 + color.green * 587 + color.blue * 114) / 1000.0
    }

    private fun checkEnglishLetter(word: String): Boolean {
        for (char in word) {
            if (char in 'a'..'z' || char in 'A'..'Z') {
                return true
            }
        }
        return false
    }

    private fun translit(str: String): String {
        val charMap = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d", 'е' to "e",
            'ё' to "yo", 'є' to "ie", 'ж' to "zh", 'з' to "z", 'и' to "i", 'і' to "i", 'ї' to "i", 'й' to "i",
            'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
            'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
            'ш' to "sh", 'щ' to "shch", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "iu", 'я' to "ia",
            'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Ґ' to "G", 'Д' to "D", 'Е' to "E",
            'Ё' to "Yo", 'Є' to "Ye", 'Ж' to "Zh", 'З' to "Z", 'И' to "I", 'І' to "I", 'Ї' to "Yi", 'Й' to "Y",
            'К' to "K", 'Л' to "L", 'М' to "M", 'Н' to "N", 'О' to "O", 'П' to "P", 'Р' to "R",
            'С' to "S", 'Т' to "T", 'У' to "U", 'Ф' to "F", 'Х' to "Kh", 'Ц' to "Ts", 'Ч' to "Ch",
            'Ш' to "Sh", 'Щ' to "Shch", 'Ы' to "Y", 'Ь' to "", 'Э' to "E", 'Ю' to "Yu", 'Я' to "Ya",
        )

        return str.lowercase(Locale.getDefault())
            .map { charMap[it] ?: if (it.isLetterOrDigit()) it.toString() else "-" }
            .joinToString("")
            .dropLastWhile { it == '-' }
    }
}
