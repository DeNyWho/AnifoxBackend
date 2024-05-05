package club.anifox.backend.service.anime.components.episodes

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuDefaultResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.episode.KitsuEpisodeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikEpisodeDto
import club.anifox.backend.domain.dto.translate.edge.TranslateTextDto
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.translate.TranslatedText
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.EpisodeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeEpisodeTranslationRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationCountRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.service.anime.components.jikan.JikanComponent
import club.anifox.backend.service.anime.components.kitsu.KitsuComponent
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.anime.translate.TranslateComponent
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class EpisodesComponent {

    @Autowired
    private lateinit var jikanComponent: JikanComponent

    @Autowired
    private lateinit var kitsuComponent: KitsuComponent

    @Autowired
    private lateinit var kodikComponent: KodikComponent

    @Autowired
    private lateinit var translateComponent: TranslateComponent

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var animeEpisodeTranslationRepository: AnimeEpisodeTranslationRepository

    @Autowired
    private lateinit var animeTranslationRepository: AnimeTranslationRepository

    @Autowired
    private lateinit var animeTranslationCountRepository: AnimeTranslationCountRepository

    fun translationsCount(episodes: List<AnimeEpisodeTable>): List<AnimeEpisodeTranslationCountTable> {
        val translationsAll = animeTranslationRepository.findAll()
        val translationsCountMap = episodes
            .flatMap { it.translations }
            .groupBy { it.translation.id }
            .map { (id, translations) ->
                AnimeEpisodeTranslationCountTable(
                    translation = translationsAll.find { it.id == id }!!,
                    countEpisodes = translations.size,
                )
            }

        return animeTranslationCountRepository.saveAll(translationsCountMap)
    }

    fun fetchEpisodes(shikimoriId: Int, kitsuId: String, type: AnimeType, urlLinkPath: String, defaultImage: String): List<AnimeEpisodeTable> {
        val jikanEpisodes = mutableListOf<JikanEpisodeDto>()
        val kitsuEpisodes = mutableListOf<KitsuEpisodeDto>()
        val episodesReady = mutableListOf<AnimeEpisodeTable>()

        val translations = animeTranslationRepository.findAll().map { it.id }.joinToString(", ")

        val kodikAnime = kodikComponent.checkKodikSingle(shikimoriId, translations)

        when (type) {
            AnimeType.Tv, AnimeType.Music, AnimeType.Ona, AnimeType.Ova, AnimeType.Special -> {
                kodikAnime.seasons.forEach { kodikSeason ->
                    if (kodikSeason.key != "0") {
                        runBlocking {
                            val deferredKitsu = async {
                                val kitsuAsyncTask = async { kitsuComponent.fetchKitsuEpisodes("api${Constants.KITSU_EDGE}${Constants.KITSU_ANIME}/${kitsuId}${Constants.KITSU_EPISODES}") }

                                var responseKitsuEpisodes = kitsuAsyncTask.await()
                                while (responseKitsuEpisodes.data != null) {
                                    kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
                                    val kitsuUrl = responseKitsuEpisodes.links.next?.replace("https://${Constants.KITSU}", "").toString()
                                    responseKitsuEpisodes = if (kitsuUrl != "null") kitsuComponent.fetchKitsuEpisodes(kitsuUrl) else KitsuDefaultResponseDto()
                                }
                            }

                            val deferredJikan = async {
                                val jikanAsyncTask = async { jikanComponent.fetchJikanEpisodes(1, shikimoriId) }

                                var responseJikanEpisodes = jikanAsyncTask.await()
                                var page = 1
                                jikanEpisodes.addAll(responseJikanEpisodes.data)
                                while (responseJikanEpisodes.data.isNotEmpty()) {
                                    page++
                                    responseJikanEpisodes = jikanComponent.fetchJikanEpisodes(page, shikimoriId)
                                    jikanEpisodes.addAll(responseJikanEpisodes.data)
                                }
                            }

                            deferredJikan.await()
                            deferredKitsu.await()
                        }

                        episodesReady.addAll(
                            runBlocking {
                                processEpisodes(
                                    shikimoriId,
                                    urlLinkPath,
                                    kodikSeason.value.episodes,
                                    kitsuEpisodes,
                                    jikanEpisodes,
                                    defaultImage,
                                    translations,
                                    type,
                                )
                            },
                        )
                    }
                }
                if (kodikAnime.seasons.isEmpty()) {
                    if (kodikAnime.link.isNotEmpty()) {
                        episodesReady.addAll(
                            runBlocking {
                                processEpisodes(
                                    shikimoriId,
                                    urlLinkPath,
                                    mapOf(Pair("1", KodikEpisodeDto(link = kodikAnime.link, screenshots = listOf()))),
                                    kitsuEpisodes,
                                    jikanEpisodes,
                                    defaultImage,
                                    translations,
                                    type,
                                )
                            },
                        )
                    }
                }
            }

            else -> {
                episodesReady.addAll(
                    runBlocking {
                        processEpisodes(
                            shikimoriId,
                            urlLinkPath,
                            mapOf(Pair("1", KodikEpisodeDto(link = kodikAnime.link, screenshots = listOf()))),
                            kitsuEpisodes,
                            jikanEpisodes,
                            defaultImage,
                            translations,
                            type,
                        )
                    },
                )
            }
        }

        return episodesReady
    }

    private suspend fun processEpisodes(
        shikimoriId: Int,
        urlLinking: String,
        kodikEpisodes: Map<String, KodikEpisodeDto>,
        kitsuEpisodes: List<KitsuEpisodeDto>,
        jikanEpisodes: List<JikanEpisodeDto>,
        imageDefault: String,
        translations: String,
        type: AnimeType,
    ): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()

        val kitsuEpisodesMapped = mutableMapOf<String, KitsuEpisodeDto?>()
        val translatedTitleMapped = mutableMapOf<String, String>()
        val translatedDescriptionMapped = mutableMapOf<String, String>()
        val tempTranslatedTitle = mutableMapOf<String, TranslateTextDto>()
        val tempTranslatedDescription = mutableMapOf<String, TranslateTextDto>()

        if (jikanEpisodes.size >= kodikEpisodes.size) {
            jikanEpisodes.map { episode ->
                val number = episode.id
                val kitsuEpisode = findEpisodeByNumber(number, kitsuEpisodes)
                if (kitsuEpisode != null) {
                    if (kitsuEpisode.attributes?.number == number) {
                        kitsuEpisodesMapped[number.toString()] = kitsuEpisode
                        translatedTitleMapped[number.toString()] = jikanEpisodes[number - 1].title
                        translatedDescriptionMapped[number.toString()] = kitsuEpisode.attributes.description ?: ""
                    }
                }
            }
        } else {
            kodikEpisodes.map { (episodeKey, episode) ->
                val number = episodeKey.toInt()
                val kitsuEpisode = findEpisodeByNumber(number, kitsuEpisodes)
                if (kitsuEpisode != null) {
                    if (kitsuEpisode.attributes?.number == number) {
                        kitsuEpisodesMapped[episodeKey] = kitsuEpisode
                        translatedDescriptionMapped[episodeKey] = kitsuEpisode.attributes.description ?: ""
                    }
                }
                if (jikanEpisodes.isNotEmpty()) {
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
        }

        translatedTitleMapped.map { (episodeKey, title) ->
            tempTranslatedTitle.put(episodeKey, TranslateTextDto(title))
        }

        translatedDescriptionMapped.map { (episodeKey, description) ->
            tempTranslatedDescription.put(episodeKey, TranslateTextDto(description))
        }

        val translateTitle = translateChunks(tempTranslatedTitle.values.toList())

        val translateDescription = translateChunks(tempTranslatedDescription.values.toList())

        translatedTitleMapped.map { (episodeKey, title) ->
            val episodeKeyList = if (translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
            translatedTitleMapped[episodeKey] = if (translateTitle[episodeKeyList].text.toString() == "null") "" else translateTitle[episodeKeyList].text.toString()
        }

        translatedDescriptionMapped.map { (episodeKey, title) ->
            val number = if (translatedDescriptionMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
            translatedDescriptionMapped[episodeKey] = if (translateDescription[number].text.toString() == "null") "" else translateDescription[number].text.toString()
        }

        val jobs = kodikEpisodes.map { (episodeKey, episode) ->
            CoroutineScope(Dispatchers.Default).async {
                processEpisode(
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

        episodeReady.addAll(addEpisodeTranslations(sortedEpisodes, shikimoriId, type, translations))

        return episodeReady
    }

    private suspend fun processEpisode(
        url: String,
        episode: Int,
        kitsuEpisode: KitsuEpisodeDto?,
        titleRu: String?,
        descriptionRu: String?,
        imageDefault: String,
        jikanEpisode: JikanEpisodeDto?,
    ): AnimeEpisodeTable {
        return if (kitsuEpisode != null) {
            val imageEpisode = try {
                when {
                    kitsuEpisode.attributes?.thumbnail?.large != null -> {
                        imageService.saveFileInSThird(
                            "images/anime/episodes/$url/${mdFive(episode.toString())}.png",
                            URL(kitsuEpisode.attributes.thumbnail.large).readBytes(),
                            compress = false,
                        )
                    }
                    kitsuEpisode.attributes?.thumbnail?.original != null -> {
                        imageService.saveFileInSThird(
                            "images/anime/episodes/$url/${mdFive(episode.toString())}.png",
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

    private fun addEpisodeTranslations(episodes: List<AnimeEpisodeTable>, shikimoriId: Int, type: AnimeType, translationsId: String): List<AnimeEpisodeTable> {
        val animeVariations = kodikComponent.checkKodikVariety(shikimoriId.toInt(), translationsId)

        val episodeTranslationsToSave = mutableListOf<EpisodeTranslationTable>()

        animeVariations.forEach { anime ->
            val translationId = when (anime.translation.id) {
                1002 -> 643
                else -> anime.translation.id
            }
            val translation = animeTranslationRepository.findById(translationId).get()

            when (type) {
                AnimeType.Tv, AnimeType.Music, AnimeType.Ona, AnimeType.Ova, AnimeType.Special -> {
                    if (anime.seasons.values.isEmpty()) {
                        if (anime.link.isNotEmpty()) {
                            val episode = episodes.first()
                            episode.let {
                                val episodeTranslation = EpisodeTranslationTable(
                                    translation = translation,
                                    link = anime.link,
                                )
                                episode.addTranslation(episodeTranslation)
                                episodeTranslationsToSave.add(episodeTranslation)
                            }
                        }
                    } else {
                        val episodeNumbers = anime.seasons.values
                            .flatMap { it.episodes.keys.mapNotNull { key -> key.toIntOrNull() } }

                        episodeNumbers.forEach { episodeNumber ->
                            val episode = episodes.find { it.number == episodeNumber }
                            episode?.let {
                                val episodeTranslation = EpisodeTranslationTable(
                                    translation = translation,
                                    link = "${anime.link}?episode=${episode.number}",
                                )
                                episode.addTranslation(episodeTranslation)
                                episodeTranslationsToSave.add(episodeTranslation)
                            }
                        }
                    }
                }
                else -> {
                    val episode = episodes.first()
                    episode.let {
                        val episodeTranslation = EpisodeTranslationTable(
                            translation = translation,
                            link = anime.link,
                        )
                        episode.addTranslation(episodeTranslation)
                        episodeTranslationsToSave.add(episodeTranslation)
                    }
                }
            }
        }

        animeEpisodeTranslationRepository.saveAll(episodeTranslationsToSave)
        return episodes
    }

    suspend fun translateChunks(texts: List<TranslateTextDto>, chunkSize: Int = 10): List<TranslatedText> {
        val tempList = mutableListOf<TranslatedText>()

        texts.chunked(chunkSize).forEach { chunk ->
            val translated = translateComponent.translateText(chunk)
            tempList.addAll(translated)
        }

        return tempList
    }

    private fun findEpisodeByNumber(number: Int, kitsuEpisodes: List<KitsuEpisodeDto>): KitsuEpisodeDto? {
        return kitsuEpisodes.find { kitsuEpisode ->
            kitsuEpisode.attributes?.number == number
        }
    }
}
