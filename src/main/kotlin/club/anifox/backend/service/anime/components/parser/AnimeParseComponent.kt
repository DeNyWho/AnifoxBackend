package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriAnimeIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMangaIdDto
import club.anifox.backend.domain.enums.anime.AnimeExternalLinksType
import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeFranchiseTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.common.AnimeRelatedTable
import club.anifox.backend.jpa.entity.anime.common.AnimeSimilarTable
import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.common.AnimeVideoTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedByStudioRepository
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.jpa.repository.anime.AnimeCharacterRepository
import club.anifox.backend.jpa.repository.anime.AnimeCharacterRoleRepository
import club.anifox.backend.jpa.repository.anime.AnimeErrorParserRepository
import club.anifox.backend.jpa.repository.anime.AnimeExternalLinksRepository
import club.anifox.backend.jpa.repository.anime.AnimeFranchiseRepository
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeRelatedRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.anime.AnimeSimilarRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.jpa.repository.anime.AnimeTranslationRepository
import club.anifox.backend.jpa.repository.anime.AnimeVideoRepository
import club.anifox.backend.service.anime.components.episodes.EpisodesComponent
import club.anifox.backend.service.anime.components.haglund.HaglundComponent
import club.anifox.backend.service.anime.components.jikan.JikanComponent
import club.anifox.backend.service.anime.components.kodik.KodikComponent
import club.anifox.backend.service.anime.components.shikimori.AnimeShikimoriComponent
import club.anifox.backend.service.anime.components.translate.TranslateComponent
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse.async
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

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
    private val animeStudiosRepository: AnimeStudiosRepository,
    private val animeErrorParserRepository: AnimeErrorParserRepository,
    private val animeRepository: AnimeRepository,
    private val animeVideoRepository: AnimeVideoRepository,
    private val animeRelatedRepository: AnimeRelatedRepository,
    private val animeSimilarRepository: AnimeSimilarRepository,
    private val animeFranchiseRepository: AnimeFranchiseRepository,
    private val animeExternalLinksRepository: AnimeExternalLinksRepository,
    private val animeCharacterRoleRepository: AnimeCharacterRoleRepository,
    private val animeCharacterRepository: AnimeCharacterRepository,
    private val animeTranslationRepository: AnimeTranslationRepository,
    private val jikanComponent: JikanComponent,
    private val translateComponent: TranslateComponent,
    private val imageService: ImageService,
) {
    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика", "Юри", "юри")

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val genreCache = ConcurrentHashMap<String, AnimeGenreTable>()
    private val studioCache = ConcurrentHashMap<String, AnimeStudioTable>()

    @Async
    fun addDataToDB() = runBlocking {
        val translationsIds = animeTranslationRepository.findAll().map { it.id }
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQueryShikimori: CriteriaQuery<Int> = criteriaBuilder.createQuery(Int::class.java)
        val shikimoriRoot = criteriaQueryShikimori.from(AnimeTable::class.java)
        criteriaQueryShikimori.select(shikimoriRoot.get("shikimoriId"))
        val query = entityManager.createQuery(criteriaQueryShikimori)
        val shikimoriIds = query.resultList

        var ar = kodikComponent.checkKodikList(translationsIds.joinToString(", "))
        while (ar.nextPage != null) {
            val jobs = ar.result
                .distinctBy { it.shikimoriId }
                .filter { !shikimoriIds.contains(it.shikimoriId) }
                .map { animeTemp ->
                    async { processData(animeTemp) }
                }
            jobs.awaitAll()
            ar = client.get(ar.nextPage!!) {
                headers { contentType(ContentType.Application.Json) }
            }.body()
        }
    }

    @Async
    fun integrations() {
        runBlocking {
            val integrationJobs = listOf(
                async { integrateSimilarRelatedFranchise() },
                async { integrateCharacters() },
            )
            integrationJobs.awaitAll()
        }
    }

    private suspend fun integrateSimilarRelatedFranchise() {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQueryShikimori: CriteriaQuery<Int> = criteriaBuilder.createQuery(Int::class.java)
        val shikimoriRoot = criteriaQueryShikimori.from(AnimeTable::class.java)
        criteriaQueryShikimori.select(shikimoriRoot.get("shikimoriId"))

        val query = entityManager.createQuery(criteriaQueryShikimori)
        val shikimoriIds = query.resultList

        for (shikimoriId in shikimoriIds) {
            try {
                val criteriaQuery: CriteriaQuery<AnimeTable> = criteriaBuilder.createQuery(AnimeTable::class.java)
                val root = criteriaQuery.from(AnimeTable::class.java)

                root.fetch<AnimeSimilarTable, Any>("similar", JoinType.LEFT)
                root.fetch<AnimeRelatedTable, Any>("related", JoinType.LEFT)
                root.fetch<AnimeFranchiseTable, Any>("franchiseMultiple", JoinType.LEFT)

                criteriaQuery.select(root)
                    .where(criteriaBuilder.equal(root.get<Int>("shikimoriId"), shikimoriId))

                val anime = entityManager
                    .createQuery(criteriaQuery)
                    .resultList
                    .first()

                val similarShikimoriIds = shikimoriComponent.fetchSimilar(shikimoriId)
                val relatedShikimori = shikimoriComponent.fetchRelated(shikimoriId)
                val franchiseShikimori = shikimoriComponent.fetchFranchise(shikimoriId)

                val similar = animeSimilarRepository.saveAll(
                    similarShikimoriIds.mapNotNull { id ->
                        val animeToSimilar = animeRepository.findByShikimoriId(id)
                        if (animeToSimilar.isPresent) {
                            val existingSimilar = anime.similar.any { it.similarAnime.id == animeToSimilar.get().id }
                            if (!existingSimilar) {
                                AnimeSimilarTable(
                                    anime = anime,
                                    similarAnime = animeToSimilar.get(),
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    },
                )

                val franchise = if (anime.franchise != null) {
                    animeFranchiseRepository.saveAll(
                        franchiseShikimori.links.mapNotNull { fran ->
                            val animeToTarget = animeRepository.findByShikimoriId(fran.targetId)
                            val animeToSource = animeRepository.findByShikimoriId(fran.sourceId)
                            if (animeToTarget.isPresent && animeToSource.isPresent) {
                                val existingFranchise = anime.franchiseMultiple.any {
                                    it.target.shikimoriId == animeToTarget.get().shikimoriId &&
                                        it.source.shikimoriId == animeToSource.get().shikimoriId
                                }
                                if (!existingFranchise) {
                                    val relationType = when (fran.relation) {
                                        "sequel" -> AnimeRelationFranchise.Sequel
                                        "prequel" -> AnimeRelationFranchise.Prequel
                                        "side_story" -> AnimeRelationFranchise.SideStory
                                        "parent_story", "full_story" -> AnimeRelationFranchise.SideStory
                                        "summary" -> AnimeRelationFranchise.Summary
                                        "alternative_version" -> AnimeRelationFranchise.AlternativeVersion
                                        "adaptation" -> AnimeRelationFranchise.Adaptation
                                        "alternative_setting" -> AnimeRelationFranchise.AlternativeSetting
                                        "spin_off" -> AnimeRelationFranchise.SpinOff
                                        "character" -> AnimeRelationFranchise.Character
                                        else -> AnimeRelationFranchise.Other
                                    }
                                    AnimeFranchiseTable(
                                        source = animeToSource.get(),
                                        target = animeToTarget.get(),
                                        relationType = relationType,
                                        relationTypeRus = relationType.russian,
                                        urlPath = anime.franchise!!,
                                    )
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        },
                    )
                } else {
                    null
                }

                val related = animeRelatedRepository.saveAll(
                    relatedShikimori.mapNotNull { relation ->
                        val id = when (val media = relation.anime ?: relation.manga) {
                            is ShikimoriAnimeIdDto -> media.id
                            is ShikimoriMangaIdDto -> return@mapNotNull null
                            else -> throw IllegalArgumentException("Неизвестный тип медиа")
                        }
                        val animeToRelation = animeRepository.findByShikimoriId(id)
                        if (animeToRelation.isPresent) {
                            val existingRelation = anime.related.any { it.relatedAnime.id == animeToRelation.get().id }
                            if (!existingRelation) {
                                AnimeRelatedTable(
                                    anime = anime,
                                    type = relation.relationRussian.toString(),
                                    relatedAnime = animeToRelation.get(),
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    },
                )

                if (similar.isNotEmpty() || related.isNotEmpty() || !franchise.isNullOrEmpty()) {
                    anime.addSimilar(similar)
                    anime.addRelation(related)
                    franchise?.let { anime.addFranchiseMultiple(it) }
                    animeRepository.saveAndFlush(anime)
                }
            } catch (e: Exception) {
                animeErrorParserRepository.save(
                    AnimeErrorParserTable(
                        message = e.message,
                        cause = "INTEGRATE SIMILAR, RELATED",
                        shikimoriId = shikimoriId,
                    ),
                )
            }
        }
    }

    private suspend fun integrateCharacters() {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQueryShikimori: CriteriaQuery<Int> = criteriaBuilder.createQuery(Int::class.java)
        val shikimoriRoot = criteriaQueryShikimori.from(AnimeTable::class.java)
        criteriaQueryShikimori.select(shikimoriRoot.get("shikimoriId"))

        val query = entityManager.createQuery(criteriaQueryShikimori)
        val shikimoriIds = query.resultList

        shikimoriIds.forEach Loop@{ shikimoriId ->
            try {
                withContext(Dispatchers.IO) {
                    val newBuilder = entityManager.criteriaBuilder
                    val criteriaQuery: CriteriaQuery<AnimeTable> = newBuilder.createQuery(AnimeTable::class.java)
                    val root = criteriaQuery.from(AnimeTable::class.java)

                    root.fetch<AnimeCharacterRoleTable, Any>("characterRoles", JoinType.LEFT)

                    criteriaQuery.select(root)
                        .where(newBuilder.equal(root.get<Int>("shikimoriId"), shikimoriId))

                    val queryTest = entityManager.createQuery(criteriaQuery)
                    val listAnime = queryTest.resultList
                    val anime = listAnime.first()

                    val animeCharactersIdsDeferred = jikanComponent.fetchJikanAnimeCharacters(shikimoriId)

                    animeCharactersIdsDeferred.data.forEach { characterData ->
                        val characterId = characterData.character.malId

                        val criteriaQueryCharacter: CriteriaQuery<AnimeCharacterTable> = newBuilder.createQuery(AnimeCharacterTable::class.java)
                        val rootCharacter = criteriaQueryCharacter.from(AnimeCharacterTable::class.java)

                        rootCharacter.fetch<AnimeCharacterRoleTable, Any>("characterRoles", JoinType.LEFT)

                        criteriaQueryCharacter.select(rootCharacter)
                        criteriaQueryCharacter.where(newBuilder.equal(rootCharacter.get<Int>("malId"), characterId))

                        val resultList = entityManager.createQuery(criteriaQueryCharacter).resultList
                        val existingCharacter = resultList.firstOrNull()

                        if (existingCharacter != null) {
                            val characterRole = AnimeCharacterRoleTable(
                                anime = anime,
                                character = existingCharacter,
                                role = translateComponent.translateSingleText("${characterData.role} role").replace("роль", "").replace("роли", "").replace("ролей", "").replace("ролям", "").dropLast(1),
                                roleEn = characterData.role,
                            )

                            val existingRole = animeCharacterRoleRepository.findByAnimeIdAndCharacterId(
                                anime.id,
                                existingCharacter.id,
                            )
                            if (existingRole != null) {
                                return@forEach
                            }

                            animeCharacterRoleRepository.save(characterRole)
                            anime.characterRoles.add(characterRole)
                            animeRepository.saveAndFlush(anime)
                        } else {
                            val character = jikanComponent.fetchJikanCharacter(characterId)

                            val characterPictures = jikanComponent.fetchJikanCharacterPictures(characterId)

                            val picturesReady = characterPictures.data.map { image ->
                                imageService.saveFileInSThird(
                                    filePath = "images/anime/${CompressAnimeImageType.CharacterImage.path}/${anime.url}/pictures/${mdFive(image.jikanJpg.imageUrl)}.${CompressAnimeImageType.CharacterImage.imageType.textFormat()}",
                                    data = URL(image.jikanJpg.imageUrl).readBytes(),
                                    compress = false,
                                    newImage = true,
                                    type = CompressAnimeImageType.CharacterImage,
                                )
                            }

                            val newCharacter = AnimeCharacterTable(
                                malId = characterId,
                                name = translateComponent.translateSingleText(character.data.name),
                                nameEn = character.data.name,
                                nameKanji = character.data.nameKanji,
                                image = imageService.saveFileInSThird(
                                    filePath = "images/anime/${CompressAnimeImageType.CharacterImage.path}/${anime.url}/${mdFive(character.data.images.jikanJpg.imageUrl)}.${CompressAnimeImageType.CharacterImage.imageType.textFormat()}",
                                    data = URL(character.data.images.jikanJpg.imageUrl).readBytes(),
                                    compress = false,
                                    newImage = true,
                                    type = CompressAnimeImageType.CharacterImage,
                                ),
                                aboutEn = character.data.about,
                                aboutRu = character.data.about?.let { translateComponent.translateSingleText(it) },
                                pictures = picturesReady.toMutableList(),
                            )
                            animeCharacterRepository.save(newCharacter)

                            val characterRole = AnimeCharacterRoleTable(
                                anime = anime,
                                character = newCharacter,
                                role = translateComponent.translateSingleText("${characterData.role} role").replace("роль", "").replace("роли", "").replace("ролей", "").replace("ролям", "").dropLast(1),
                                roleEn = characterData.role,
                            )

                            val existingRole = animeCharacterRoleRepository.findByAnimeIdAndCharacterId(
                                anime.id,
                                newCharacter.id,
                            )
                            if (existingRole != null) {
                                return@forEach
                            }

                            animeCharacterRoleRepository.save(characterRole)
                            anime.characterRoles.add(characterRole)
                            newCharacter.characterRoles.add(characterRole)
                            animeRepository.saveAndFlush(anime)
                        }
                    }
                }
            } catch (e: Exception) {
                animeErrorParserRepository.save(
                    AnimeErrorParserTable(
                        message = e.message,
                        cause = "INTEGRATE CHARACTER",
                        shikimoriId = shikimoriId,
                    ),
                )
                return@Loop
            }
        }
    }

    private suspend fun processData(animeKodik: KodikAnimeDto) {
        coroutineScope {
            try {
                val shikimori = shikimoriComponent.fetchAnime(animeKodik.shikimoriId)
                val jikan = jikanComponent.fetchJikan(animeKodik.shikimoriId)

                if (shikimori != null) {
                    val shikimoriRating = shikimori.score.toDoubleOrNull() ?: 0.0
                    val userRatesStats = shikimori.usersRatesStats.sumOf { it.value }

                    runBlocking {
                        if (!animeBlockedRepository.findById(shikimori.id).isPresent &&
                            (userRatesStats > 4000 || (userRatesStats > 1000 && shikimori.status == "ongoing")) &&
                            shikimori.studios.none { studio ->
                                animeBlockedByStudioRepository.findById(studio.id).isPresent
                            } &&
                            !animeRepository.findByShikimoriId(shikimori.id).isPresent
                        ) {
                            val (genres, studios) = processGenresAndStudios(shikimori)

                            val type =
                                when (shikimori.kind) {
                                    "movie" -> AnimeType.Movie
                                    "tv" -> AnimeType.Tv
                                    "ova" -> AnimeType.Ova
                                    "ona" -> AnimeType.Ona
                                    "special" -> AnimeType.Special
                                    "music" -> AnimeType.Music
                                    else -> AnimeType.Tv
                                }

                            val airedOn = LocalDate.parse(shikimori.airedOn)
                            val releasedOn =
                                when {
                                    shikimori.releasedOn != null -> {
                                        LocalDate.parse(shikimori.releasedOn)
                                    }

                                    type == AnimeType.Movie -> {
                                        LocalDate.parse(shikimori.airedOn)
                                    }

                                    else -> null
                                }

                            val animeIdsDeferred =
                                async {
                                    haglundComponent.fetchHaglundIds(shikimori.id)
                                }

                            var urlLinkPath = commonParserComponent.translit(if (!shikimori.russianLic.isNullOrEmpty() && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)

                            if (animeRepository.findByUrl(urlLinkPath).isPresent) {
                                urlLinkPath = "${commonParserComponent.translit(if (shikimori.russianLic != null && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)}-${airedOn.year}"
                            }

                            val videosShikimoriDeferred =
                                async {
                                    shikimoriComponent.fetchVideos(shikimori.id)
                                }

                            val shikimoriScreenshotsDeferred =
                                async {
                                    shikimoriComponent.fetchScreenshots(shikimori.id)
                                }

                            val shikimoriExternalLinksDeferred =
                                async {
                                    shikimoriComponent.fetchExternalLinks(shikimori.id)
                                }

                            val status =
                                when (shikimori.status) {
                                    "released" -> AnimeStatus.Released
                                    "ongoing" -> AnimeStatus.Ongoing
                                    else -> AnimeStatus.Ongoing
                                }

                            val season =
                                when (airedOn.month.value) {
                                    12, 1, 2 -> AnimeSeason.Winter
                                    3, 4, 5 -> AnimeSeason.Spring
                                    6, 7, 8 -> AnimeSeason.Summer
                                    else -> AnimeSeason.Fall
                                }

                            val ratingMpa =
                                when (shikimori.rating) {
                                    "g" -> "G"
                                    "pg" -> "PG"
                                    "pg_13" -> "PG-13"
                                    "r" -> "R"
                                    "r_plus" -> "R+"
                                    else -> ""
                                }

                            val minimalAge =
                                when (shikimori.rating) {
                                    "g" -> 0
                                    "pg" -> 12
                                    "pg_13" -> 16
                                    "r" -> 18
                                    "r_plus" -> 18
                                    else -> 0
                                }

                            val formatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                    .withZone(ZoneId.of("Europe/Moscow"))

                            val nextEpisode = if (shikimori.nextEpisodeAt != null) LocalDateTime.parse(shikimori.nextEpisodeAt, formatter) else null

                            val otherTitles = animeKodik.materialData.otherTitles.toMutableList()
                            otherTitles.add(if (shikimori.russianLic != null && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)

                            val animeIds = animeIdsDeferred.await()

                            val imagesDeferred =
                                async {
                                    fetchImageComponent.fetchAndSaveAnimeImages(shikimori.id, animeIds.kitsu, urlLinkPath)
                                }

                            val (images, bufferedLargeImage) = imagesDeferred.await() ?: return@runBlocking

                            val licensors = shikimori.licensors.filter { it == "DEEP" || it == "Экспонента" || it == "Вольга" }
                            val isLicensed = licensors.isNotEmpty()

                            val episodesReady =
                                if (!isLicensed) {
                                    episodesComponent.fetchEpisodes(
                                        shikimoriId = shikimori.id,
                                        kitsuId = animeIds.kitsu.toString(),
                                        type = type,
                                        urlLinkPath = urlLinkPath,
                                        defaultImage = images.medium,
                                    )
                                } else {
                                    null
                                }

                            val translationsCountReady =
                                if (episodesReady != null) {
                                    episodesComponent.translationsCount(episodesReady)
                                } else {
                                    null
                                }

                            val translations = translationsCountReady?.map { it.translation }

                            val videos =
                                videosShikimoriDeferred.await().let { videosList ->
                                    animeVideoRepository.saveAll(
                                        videosList
                                            .filter { it.hosting == "youtube" && it.kind != "episode_preview" }
                                            .map { video ->
                                                AnimeVideoTable(
                                                    url = video.url,
                                                    imageUrl = video.imageUrl,
                                                    playerUrl = video.playerUrl,
                                                    name = video.name,
                                                    type =
                                                    when (video.kind) {
                                                        "ed" -> AnimeVideoType.Ending
                                                        "op" -> AnimeVideoType.Opening
                                                        "pv" -> AnimeVideoType.Trailer
                                                        else -> AnimeVideoType.Other
                                                    },
                                                )
                                            },
                                    )
                                }

                            jikan.data.trailer?.let { trailer ->
                                if (trailer.url != null && trailer.embedUrl != null && trailer.images?.maximumImageUrl != null) {
                                    videos.add(
                                        AnimeVideoTable(
                                            url = trailer.url,
                                            imageUrl = trailer.images.maximumImageUrl,
                                            playerUrl = trailer.embedUrl.replace("?enablejsapi=1&wmode=opaque&autoplay=1", ""),
                                            name = null,
                                            type = AnimeVideoType.MainTrailer,
                                        ),
                                    )
                                }
                            }

                            val screenshots =
                                shikimoriScreenshotsDeferred.await().map { screenshot ->
                                    fetchImageComponent.saveImage(screenshot, CompressAnimeImageType.Screenshot, urlLinkPath, true)
                                }.toMutableList()

                            val externalLinks =
                                shikimoriExternalLinksDeferred.await().let { externalList ->
                                    animeExternalLinksRepository.saveAll(
                                        externalList
                                            .filter { it.kind == "official_site" || it.kind == "wikipedia" || it.kind == "kinopoisk" || it.kind == "ivi" || it.kind == "wink" || it.kind == "okko" }
                                            .map { external ->
                                                AnimeExternalLinksTable(
                                                    kind =
                                                    when (external.kind) {
                                                        "official_site" -> AnimeExternalLinksType.OfficialSite
                                                        "wikipedia" -> AnimeExternalLinksType.Wikipedia
                                                        "kinopoisk" -> AnimeExternalLinksType.Kinopoisk
                                                        "ivi" -> AnimeExternalLinksType.Ivi
                                                        "wink" -> AnimeExternalLinksType.Wink
                                                        "okko" -> AnimeExternalLinksType.Okko
                                                        else -> AnimeExternalLinksType.Okko
                                                    },
                                                    url = external.url,
                                                )
                                            },
                                    )
                                }

                            var episodesCount =
                                when {
                                    episodesReady != null && shikimori.episodes < episodesReady.size -> episodesReady.size
                                    shikimori.episodes == 0 && status == AnimeStatus.Ongoing -> null
                                    else -> shikimori.episodes
                                }

                            if (episodesCount != null) {
                                if (episodesReady != null && episodesCount < episodesReady.size) {
                                    episodesCount = episodesReady.size
                                }
                            }

                            val animeToSave =
                                AnimeTable(
                                    type = type,
                                    url = urlLinkPath,
                                    playerLink = if (licensors.isEmpty()) animeKodik.link else null,
                                    title = if (!shikimori.russianLic.isNullOrEmpty() && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian,
                                    titleEn = shikimori.english.map { it.toString() }.toMutableList(),
                                    titleJapan = shikimori.japanese.toMutableList(),
                                    synonyms = shikimori.synonyms.toMutableList(),
                                    titleOther = otherTitles,
                                    ids =
                                    AnimeIdsTable(
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
                                    isLicensed = isLicensed,
                                    year = airedOn.year,
                                    nextEpisode = nextEpisode,
                                    episodesCount = episodesCount,
                                    episodesAired = episodesReady?.size,
                                    shikimoriId = shikimori.id,
                                    createdAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
                                    airedOn = airedOn,
                                    releasedOn = releasedOn,
                                    updatedAt = null,
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

                            if (translationsCountReady != null) {
                                animeToSave.addTranslationCount(translationsCountReady)
                            }
                            if (translations != null) {
                                animeToSave.addTranslation(translations)
                            }
                            if (episodesReady != null) {
                                animeToSave.addEpisodesAll(episodesReady)
                            }

                            animeToSave.nextEpisode?.let { nextEpisodeDate ->
                                animeToSave.updateEpisodeSchedule(nextEpisodeDate)
                            }

                            animeToSave.addAllAnimeGenre(genres)
                            animeToSave.addAllAnimeStudios(studios)
                            animeToSave.addVideos(videos)
                            animeToSave.addExternalLinks(externalLinks)
                            animeToSave.addLicensors(licensors)

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
                e.printStackTrace()
                animeErrorParserRepository.save(
                    AnimeErrorParserTable(
                        message = e.message,
                        cause = "parser",
                        shikimoriId = animeKodik.shikimoriId,
                    ),
                )
                return@coroutineScope
            }
        }
    }

    private suspend fun processGenresAndStudios(shikimori: ShikimoriDto): Pair<List<AnimeGenreTable>, List<AnimeStudioTable>> {
        val genres =
            shikimori.genres
                .filter { it.russian !in inappropriateGenres }
                .map { genre ->
                    genreCache.computeIfAbsent(genre.russian) {
                        animeGenreRepository.findByGenre(genre.russian)
                            .orElseGet {
                                val newGenre = AnimeGenreTable(name = genre.russian)
                                animeGenreRepository.save(newGenre)
                                newGenre
                            }
                    }
                }

        val studios =
            shikimori.studios
                .map { studio ->
                    studioCache.computeIfAbsent(studio.name) {
                        animeStudiosRepository.findByStudio(studio.name)
                            .orElseGet {
                                val newStudio = AnimeStudioTable(name = studio.name)
                                animeStudiosRepository.save(newStudio)
                                newStudio
                            }
                    }
                }

        return Pair(genres, studios)
    }
}
