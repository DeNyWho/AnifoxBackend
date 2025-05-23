package club.anifox.backend.service.anime.components.episodes

import club.anifox.backend.domain.constants.Constants
import club.anifox.backend.domain.dto.anime.jikan.JikanEpisodeDto
import club.anifox.backend.domain.dto.anime.kitsu.KitsuDefaultResponseDto
import club.anifox.backend.domain.dto.anime.kitsu.episode.KitsuEpisodeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikEpisodeDto
import club.anifox.backend.domain.dto.translate.edge.TranslateTextDto
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.EpisodeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeEpisodeTranslationRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationCountRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.service.anime.components.jikan.JikanComponent
import club.anifox.backend.service.anime.components.kitsu.KitsuComponent
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.anime.components.translate.TranslateComponent
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
        val translationsAll = animeTranslationRepository.findAll().associateBy { it.id }
        val translationsCountMap =
            episodes
                .flatMap { it.translations }
                .groupingBy { it.translation.id }
                .eachCount()
                .map { (id, count) ->
                    AnimeEpisodeTranslationCountTable(
                        translation = translationsAll[id]!!,
                        countEpisodes = count,
                    )
                }

        return animeTranslationCountRepository.saveAll(translationsCountMap)
    }

    suspend fun fetchEpisodes(
        shikimoriId: Int,
        kitsuId: String?,
        type: AnimeType,
        urlLinkPath: String,
        defaultImage: String,
        locallyEpisodes: List<AnimeEpisodeTable>,
    ): List<AnimeEpisodeTable> {
        val jikanEpisodes = mutableListOf<JikanEpisodeDto>()
        val kitsuEpisodes = mutableListOf<KitsuEpisodeDto>()
        val episodesReady = mutableListOf<AnimeEpisodeTable>()

        val translations = animeTranslationRepository.findAll().map { it.id }.joinToString(", ")
        val kodikAnime = kodikComponent.checkKodikSingle(shikimoriId, translations)

        when (type) {
            AnimeType.Tv, AnimeType.Music, AnimeType.Ona, AnimeType.Ova, AnimeType.Special -> {
                kodikAnime.seasons.forEach { kodikSeason ->
                    if (kodikSeason.key != "0") {
                        coroutineScope {
                            val deferredJikan = async { fetchJikanEpisodes(shikimoriId) }

                            if (kitsuId != null) {
                                val deferredKitsu = async { fetchKitsuEpisodes(kitsuId) }
                                kitsuEpisodes.addAll(deferredKitsu.await())
                            }

                            jikanEpisodes.addAll(deferredJikan.await())
                        }

                        episodesReady.addAll(
                            processEpisodes(
                                shikimoriId,
                                urlLinkPath,
                                kodikSeason.value.episodes,
                                kitsuEpisodes,
                                jikanEpisodes,
                                defaultImage,
                                translations,
                                type,
                                locallyEpisodes,
                            ),
                        )
                    }
                }
                if (kodikAnime.seasons.isEmpty()) {
                    if (kodikAnime.link.isNotEmpty()) {
                        episodesReady.addAll(
                            processEpisodes(
                                shikimoriId,
                                urlLinkPath,
                                mapOf(Pair("1", KodikEpisodeDto(link = kodikAnime.link, screenshots = listOf()))),
                                kitsuEpisodes,
                                jikanEpisodes,
                                defaultImage,
                                translations,
                                type,
                                locallyEpisodes,
                            ),
                        )
                    }
                }
            }

            else -> {
                episodesReady.addAll(
                    processEpisodes(
                        shikimoriId,
                        urlLinkPath,
                        mapOf(Pair("1", KodikEpisodeDto(link = kodikAnime.link, screenshots = listOf()))),
                        kitsuEpisodes,
                        jikanEpisodes,
                        defaultImage,
                        translations,
                        type,
                        locallyEpisodes,
                    ),
                )
            }
        }

        return episodesReady
    }

    private suspend fun fetchKitsuEpisodes(kitsuId: String): List<KitsuEpisodeDto> {
        val kitsuEpisodes = mutableListOf<KitsuEpisodeDto>()
        var responseKitsuEpisodes = kitsuComponent.fetchKitsuEpisodes("api${Constants.KITSU_EDGE}${Constants.KITSU_ANIME}/${kitsuId}${Constants.KITSU_EPISODES}${Constants.KITSU_PAGE_LIMIT}")
        while (responseKitsuEpisodes.data != null) {
            kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
            val kitsuUrl = responseKitsuEpisodes.links.next?.replace("https://${Constants.KITSU}", "").toString()
            responseKitsuEpisodes = if (kitsuUrl != "null") kitsuComponent.fetchKitsuEpisodes(kitsuUrl) else KitsuDefaultResponseDto()
        }
        return kitsuEpisodes
    }

    private suspend fun fetchJikanEpisodes(shikimoriId: Int): List<JikanEpisodeDto> {
        val jikanEpisodes = mutableListOf<JikanEpisodeDto>()
        var responseJikanEpisodes = jikanComponent.fetchJikanEpisodes(1, shikimoriId)
        var page = 1
        jikanEpisodes.addAll(responseJikanEpisodes.data)
        while (responseJikanEpisodes.data.isNotEmpty()) {
            page++
            responseJikanEpisodes = jikanComponent.fetchJikanEpisodes(page, shikimoriId)
            jikanEpisodes.addAll(responseJikanEpisodes.data)
        }
        return jikanEpisodes
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
        locallyEpisodes: List<AnimeEpisodeTable>,
    ): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()
        val existingEpisodesByNumber = locallyEpisodes.associateBy { it.number }

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
            kodikEpisodes.map { (episodeKey, _) ->
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
                        translatedTitleMapped[episodeKey] =
                            when (episodeKey) {
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

        val translateTitle = translateComponent.translateChunks(tempTranslatedTitle.values.toList())

        val translateDescription = translateComponent.translateChunks(tempTranslatedDescription.values.toList())

        translatedTitleMapped.map { (episodeKey, _) ->
            val episodeKeyList = if (translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
            translatedTitleMapped[episodeKey] = if (translateTitle[episodeKeyList].text.toString() == "null") "" else translateTitle[episodeKeyList].text.toString()
        }

        translatedDescriptionMapped.map { (episodeKey, _) ->
            val number = if (translatedDescriptionMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
            translatedDescriptionMapped[episodeKey] = if (translateDescription[number].text.toString() == "null") "" else translateDescription[number].text.toString()
        }

        val jobs =
            kodikEpisodes.map { (episodeKey, _) ->
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
                        existingEpisodesByNumber[episodeKey.toInt()],
                    )
                }
            }

        val processedEpisodes = jobs.awaitAll()
        val sortedEpisodes = processedEpisodes.sortedBy { it.number }

        episodeReady.addAll(
            addEpisodeTranslations(
                sortedEpisodes,
                shikimoriId,
                type,
                translations,
            ),
        )

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
        existingEpisode: AnimeEpisodeTable?,
    ): AnimeEpisodeTable {
        val imageEpisode =
            kitsuEpisode?.attributes?.thumbnail?.let { thumbnail ->
                try {
                    when {
                        thumbnail.original != null -> {
                            imageService.saveFileInSThird(
                                "images/anime/${CompressAnimeImageType.Episodes.path}/$url/${mdFive(episode.toString())}.${CompressAnimeImageType.Episodes.imageType.textFormat()}",
                                URL(thumbnail.original).readBytes(),
                                compress = false,
                                type = CompressAnimeImageType.Episodes,
                            )
                        }
                        thumbnail.large != null -> {
                            imageService.saveFileInSThird(
                                "images/anime/${CompressAnimeImageType.Episodes.path}/$url/${mdFive(episode.toString())}.${CompressAnimeImageType.Episodes.imageType.textFormat()}",
                                URL(thumbnail.large).readBytes(),
                                compress = false,
                                type = CompressAnimeImageType.Episodes,
                            )
                        }
                        else -> imageDefault
                    }
                } catch (e: Exception) {
                    imageDefault
                }
            } ?: imageDefault

        val kitsuNumber = kitsuEpisode?.attributes?.number ?: episode

        val airedDate =
            jikanEpisode?.aired?.takeIf { it.length > 3 }
                ?: kitsuEpisode?.attributes?.airDate?.takeIf { it.length > 3 }

        return existingEpisode?.apply {
            title = titleRu?.takeIf { it.length > 3 } ?: "$episode"
            titleEn = kitsuEpisode?.attributes?.titles?.enToUs ?: "$episode"
            description = descriptionRu
            descriptionEn = kitsuEpisode?.attributes?.description.orEmpty()
            duration = kitsuEpisode?.attributes?.length
            image = imageEpisode.takeIf { it.length > 5 } ?: imageDefault
            filler = jikanEpisode?.filler ?: false
            recap = jikanEpisode?.recap ?: false
            aired = airedDate?.let { LocalDate.parse(it.take(10), DateTimeFormatter.ISO_DATE) }
        }
            ?: AnimeEpisodeTable(
                title = titleRu?.takeIf { it.length > 3 } ?: "$episode",
                titleEn = kitsuEpisode?.attributes?.titles?.enToUs ?: "$episode",
                description = descriptionRu,
                descriptionEn = kitsuEpisode?.attributes?.description.orEmpty(),
                number = kitsuNumber,
                duration = kitsuEpisode?.attributes?.length,
                image = imageEpisode.takeIf { it.length > 5 } ?: imageDefault,
                filler = jikanEpisode?.filler ?: false,
                recap = jikanEpisode?.recap ?: false,
                aired = airedDate?.let { LocalDate.parse(it.take(10), DateTimeFormatter.ISO_DATE) },
            )
    }

    @Transactional
    private suspend fun addEpisodeTranslations(
        episodes: List<AnimeEpisodeTable>,
        shikimoriId: Int,
        type: AnimeType,
        translationsId: String,
    ): List<AnimeEpisodeTable> {
        val animeVariations = kodikComponent.checkKodikVariety(shikimoriId, translationsId)

        animeVariations.forEach { anime ->
            val translationId =
                when (anime.translation.id) {
                    1002 -> 643
                    else -> anime.translation.id
                }
            val translation = animeTranslationRepository.findById(translationId).get()

            when (type) {
                AnimeType.Tv, AnimeType.Music, AnimeType.Ona, AnimeType.Ova, AnimeType.Special -> {
                    if (anime.seasons.values.isEmpty()) {
                        if (anime.link.isNotEmpty() && episodes.size == 1) {
                            val episode = episodes.first()
                            val existTranslation = episode.translations.find { it.translation.id == translation.id }

                            if (existTranslation == null) {
                                val episodeTranslation = EpisodeTranslationTable(
                                    translation = translation,
                                    link = anime.link,
                                )

                                animeEpisodeTranslationRepository.saveAndFlush(episodeTranslation)

                                episodes.map {
                                    it.addTranslation(episodeTranslation)
                                }
                            }
                        }
                    } else {
                        anime.seasons.values
                            .flatMap { it.episodes.keys.mapNotNull(String::toIntOrNull) }
                            .mapNotNull { episodeNumber ->
                                episodes.find { it.number == episodeNumber }?.let { episode ->
                                    val existTranslation = episode.translations.find { it.translation.id == translation.id }

                                    if (existTranslation == null) {
                                        val episodeTranslation = EpisodeTranslationTable(
                                            translation = translation,
                                            link = "${anime.link}?episode=${episode.number}",
                                        )

                                        animeEpisodeTranslationRepository.saveAndFlush(episodeTranslation)

                                        episodes.map { episodeExist ->
                                            if (episodeExist.number == episodeNumber) {
                                                episodeExist.addTranslation(episodeTranslation)
                                            } else {
                                                episodeExist
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
                AnimeType.Movie -> {
                    val episode = episodes.first()
                    val existTranslation = episode.translations.find { it.translation.id == translation.id }

                    if (existTranslation == null) {
                        val episodeTranslation = EpisodeTranslationTable(
                            translation = translation,
                            link = anime.link,
                        )

                        animeEpisodeTranslationRepository.saveAndFlush(episodeTranslation)

                        episodes.map { episodeExist ->
                            if (episodeExist.number == episode.number) {
                                episodeExist.addTranslation(episodeTranslation)
                            } else {
                                episodeExist
                            }
                        }
                    }
                }
            }
        }

        return episodes
    }

    private fun findEpisodeByNumber(
        number: Int,
        kitsuEpisodes: List<KitsuEpisodeDto>,
    ): KitsuEpisodeDto? {
        return kitsuEpisodes.find { kitsuEpisode ->
            kitsuEpisode.attributes?.number == number
        }
    }
}
