package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriAnimeIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMangaIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriRelationDto
import club.anifox.backend.domain.enums.anime.AnimeMusicType
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeBufferedImages
import club.anifox.backend.domain.model.anime.AnimeImagesTypes
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.AnimeMediaTable
import club.anifox.backend.jpa.entity.anime.AnimeMusicTable
import club.anifox.backend.jpa.entity.anime.AnimeRelatedTable
import club.anifox.backend.jpa.entity.anime.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeMusicRepository
import club.anifox.backend.jpa.repository.anime.AnimeRelatedRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.haglund.HaglundComponent
import club.anifox.backend.service.anime.components.jikan.JikanComponent
import club.anifox.backend.service.anime.components.kitsu.KitsuComponent
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    private lateinit var client: HttpClient

    @Autowired
    private lateinit var kodikComponent: KodikComponent

    @Autowired
    private lateinit var jikanComponent: JikanComponent

    @Autowired
    private lateinit var kitsuComponent: KitsuComponent

    @Autowired
    private lateinit var haglundComponent: HaglundComponent

    @Autowired
    private lateinit var shikimoriComponent: AnimeShikimoriComponent

    @Autowired
    private lateinit var episodesComponent: EpisodesComponent

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
    private lateinit var animeErrorParserRepository: AnimeErrorParserRepository

    @Autowired
    private lateinit var animeMusicRepository: AnimeMusicRepository

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var imageService: ImageService

    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика")

    fun addDataToDB() {
        var nextPage: String? = "1"
        val translationsIds = animeTranslationRepository.findAll().map { it.id }.joinToString(", ")
        var ar = runBlocking {
            kodikComponent.checkKodikList(translationsIds)
        }

        while (nextPage != null) {
            ar.result.distinctBy { it.shikimoriId }.forEach Loop@{ animeTemp ->
                try {
                    val anime = kodikComponent.checkKodikSingle(animeTemp.shikimoriId.toInt(), translationsIds)

                    val shikimori = shikimoriComponent.checkShikimori(anime.shikimoriId)

                    var userRatesStats = 0

                    shikimori?.usersRatesStats?.forEach {
                        userRatesStats += it.value
                    }

                    val shikimoriRating = try {
                        shikimori?.score?.toDouble()!!
                    } catch (_: Exception) {
                        0.0
                    }

                    if (
                        !anime.materialData.title.contains("Атака Титанов") &&
                        !anime.materialData.title.contains("Атака титанов") &&
                        !animeBlockedRepository.findById(anime.shikimoriId.toInt()).isPresent && anime.materialData.shikimoriVotes > 90 && userRatesStats > 1000 && shikimori != null &&
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
                                haglundComponent.fetchHaglundIds(anime.shikimoriId)
                            }

                            val titleRussianLic = shikimori.russianLic
                            val titleRussian = shikimori.russian

                            var urlLinking = translit(if (!titleRussianLic.isNullOrEmpty() && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian)

                            if (animeRepository.findByUrl(urlLinking).isPresent) {
                                urlLinking = "${translit(if (titleRussianLic != null && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian)}-${if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.year}"
                            }

                            val relationIdsDeferred = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    shikimoriComponent.fetchShikimoriRelated(anime.shikimoriId)
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
                                emit(shikimoriComponent.fetchShikimoriSimilar(anime.shikimoriId).take(30))
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
                                    kitsuComponent.fetchKitsuAnime(animeIds.kitsu!!)
                                }.getOrElse {
                                    null
                                }
                            }

                            val jikanImage = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    jikanComponent.fetchJikanImages(anime.shikimoriId)
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
                                                        "images/anime/large/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
                                                        URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                                        compress = true,
                                                        width = 400,
                                                        height = 640,
                                                    ),
                                                    medium = imageService.saveFileInSThird(
                                                        "images/anime/medium/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
                                                        URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                                        compress = true,
                                                        width = 200,
                                                        height = 440,
                                                    ),
                                                    cover = try {
                                                        if (kitsuData.attributesKitsu.coverImage.coverLarge != null) {
                                                            imageService.saveFileInSThird(
                                                                "images/anime/cover/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
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
                                                        "images/anime/large/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
                                                        URL(jikanData.images.jikanJpg.largeImageUrl).readBytes(),
                                                    ),
                                                    medium = imageService.saveFileInSThird(
                                                        "images/anime/medium/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
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
                                                    "images/anime/large/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
                                                    URL(jikanData.images.jikanJpg.largeImageUrl).readBytes(),
                                                ),
                                                medium = imageService.saveFileInSThird(
                                                    "images/anime/medium/$urlLinking/${mdFive(UUID.randomUUID().toString())}.png",
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

                            val type = when (anime.materialData.animeType) {
                                "movie" -> AnimeType.Movie
                                "tv" -> AnimeType.Tv
                                "ova" -> AnimeType.Ova
                                "ona" -> AnimeType.Ona
                                "special" -> AnimeType.Special
                                "music" -> AnimeType.Music
                                else -> AnimeType.Tv
                            }

                            val episodesReady = mutableListOf<AnimeEpisodeTable>()

                            episodesReady.addAll(episodesComponent.fetchEpisodes(shikimoriId = anime.shikimoriId, kitsuId = animeIds.kitsu.toString(), type = type, urlLinking = urlLinking, defaultImage = animeImages?.medium ?: ""))

                            val jikanThemesDeferred = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    jikanComponent.fetchJikanThemes(anime.shikimoriId)
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
                                emit(shikimoriComponent.fetchShikimoriScreenshots(anime.shikimoriId))
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

                            val translationsCountReady = episodesComponent.translationsCount(episodesReady)

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

                            val airedOn = shikimori.airedAt?.let { LocalDate.parse(it) }
                                ?: if (shikimori.episodes == 1) {
                                    shikimori.releasedAt?.let { LocalDate.parse(it) }
                                        ?: anime.materialData.releasedAt
                                } else {
                                    anime.materialData.airedAt
                                }

                            val a = AnimeTable(
                                title = if (!titleRussianLic.isNullOrEmpty() && checkEnglishLetter(titleRussian)) titleRussianLic else titleRussian,
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
                                titleEn = shikimori.english.map { it.toString() }.toMutableList(),
                                titleJapan = shikimori.japanese.toMutableList(),
                                synonyms = shikimori.synonyms.toMutableList(),
                                titleOther = otherTitles,
                                similarAnime = similarIds,
                                status = status,
                                description = shikimori.description.replace(Regex("\\[\\/?[a-z]+.*?\\]"), ""),
                                year = if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.airedAt.year,
                                createdAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
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
                                shikimoriRating = shikimoriRating,
                                shikimoriVotes = userRatesStats,
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
