package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.dto.anime.jikan.character.JikanAnimeCharactersDto
import club.anifox.backend.domain.dto.anime.kodik.KodikAnimeDto
import club.anifox.backend.domain.dto.anime.kodik.KodikResponseDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriAnimeIdDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriDto
import club.anifox.backend.domain.dto.anime.shikimori.ShikimoriMangaIdDto
import club.anifox.backend.domain.enums.anime.AnimeExternalLinksType
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.domain.model.anime.character.ProcessedCharacterData
import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable
import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
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
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
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
    private val animeExternalLinksRepository: AnimeExternalLinksRepository,
    private val animeCharacterRoleRepository: AnimeCharacterRoleRepository,
    private val animeCharacterRepository: AnimeCharacterRepository,
    private val animeTranslationRepository: AnimeTranslationRepository,
    private val jikanComponent: JikanComponent,
    private val translateComponent: TranslateComponent,
    private val imageService: ImageService,
) {
    private val inappropriateGenres = listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика", "Юри", "юри")
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val genreCache = ConcurrentHashMap<String, AnimeGenreTable>()
    private val studioCache = ConcurrentHashMap<String, AnimeStudioTable>()

    @Async
    fun addDataToDB() {
        runBlocking {
            try {
                logger.info("Starting Kodik data processing")
                val translationsIds = animeTranslationRepository.findAll().map { it.id }
                val shikimoriIds = fetchExistingShikimoriIds()
                var ar = kodikComponent.checkKodikList(translationsIds.joinToString(", "))

                do {
                    val batchToProcess = ar.result
                        .shuffled()
                        .distinctBy { it.shikimoriId }
                        .filter { !shikimoriIds.contains(it.shikimoriId) }

                    logger.info("Processing batch of ${batchToProcess.size} anime")

                    batchToProcess.chunked(BATCH_SIZE).forEach { chunk ->
                        try {
                            processBatch(chunk)
                        } catch (e: Exception) {
                            logger.error("Failed to process batch", e)
                        } finally {
                            entityManager.clear()
                        }
                    }

                    if (ar.nextPage != null) {
                        ar = fetchNextPage(ar.nextPage!!)
                    }
                } while (ar.nextPage != null)

                val lastBatchToProcess = ar.result
                    .distinctBy { it.shikimoriId }
                    .filter { !shikimoriIds.contains(it.shikimoriId) }

                lastBatchToProcess.chunked(BATCH_SIZE).forEach { chunk ->
                    try {
                        processBatch(chunk)
                    } catch (e: Exception) {
                        logger.error("Failed to process batch", e)
                    } finally {
                        entityManager.clear()
                    }
                }

                logger.info("Kodik data processing completed.")
            } catch (e: Exception) {
                logger.error("Critical error in Kodik data processing", e)
                throw e
            }
        }
    }

    fun integrations() = CoroutineScope(Dispatchers.IO).launch {
        val integrationJobs = listOf(
            async { integrateSimilarRelated() },
            async { integrateCharacters() },
        )
        integrationJobs.awaitAll()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private suspend fun integrateSimilarRelated() {
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

                criteriaQuery.select(root)
                    .where(criteriaBuilder.equal(root.get<Int>("shikimoriId"), shikimoriId))

                val anime = entityManager
                    .createQuery(criteriaQuery)
                    .resultList
                    .first()

                val similarShikimoriIds = shikimoriComponent.fetchSimilar(shikimoriId)
                val relatedShikimori = shikimoriComponent.fetchRelated(shikimoriId)

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

                if (similar.isNotEmpty() || related.isNotEmpty()) {
                    anime.addSimilar(similar)
                    anime.addRelation(related)
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

    @Transactional
    private suspend fun integrateCharacters() = coroutineScope {
        val shikimoriIds = entityManager.createQuery(
            "SELECT a.shikimoriId FROM AnimeTable a WHERE a.shikimoriId IS NOT NULL",
            Int::class.java,
        ).resultList

        // Обработка партиями по 2 аниме
        shikimoriIds.chunked(2).forEach { batch ->
            batch.map { shikimoriId ->
                async(Dispatchers.IO) {
                    processShikimoriIdForCharacter(shikimoriId)
                }
            }.awaitAll()
            entityManager.clear() // Очистка контекста для снижения нагрузки на память
        }
    }

    private suspend fun processShikimoriIdForCharacter(shikimoriId: Int) = coroutineScope {
        try {
            val anime = try {
                entityManager.createQuery(
                    """
            SELECT a FROM AnimeTable a
            LEFT JOIN FETCH a.characterRoles
            WHERE a.shikimoriId = :shikimoriId
            """,
                    AnimeTable::class.java,
                ).setParameter("shikimoriId", shikimoriId)
                    .singleResult
            } catch (e: NoResultException) {
                return@coroutineScope // Пропустить, если аниме не найдено
            }

            val animeCharactersData = jikanComponent.fetchJikanAnimeCharacters(shikimoriId)

            val existingCharacters = if (animeCharactersData.data.isNotEmpty()) {
                val malIds = animeCharactersData.data.map { it.character.malId }
                entityManager.createQuery(
                    """
            SELECT c FROM AnimeCharacterTable c
            WHERE c.malId IN :malIds
            """,
                    AnimeCharacterTable::class.java,
                ).setParameter("malIds", malIds)
                    .resultList
                    .associateBy { it.malId }
            } else {
                emptyMap()
            }

            val processedData = animeCharactersData.data.map { characterData ->
                val existingCharacter = existingCharacters[characterData.character.malId]
                processCharacterData(anime, characterData, existingCharacter)
            }

            saveProcessedDataBatch(processedData)
        } catch (e: Exception) {
            animeErrorParserRepository.save(
                AnimeErrorParserTable(
                    message = e.message,
                    cause = "INTEGRATE CHARACTER",
                    shikimoriId = shikimoriId,
                ),
            )
        }
    }

    @Transactional
    private suspend fun saveProcessedDataBatch(processedData: List<ProcessedCharacterData>) {
        // Сначала сохраняем новых персонажей
        val newCharacters = processedData.mapNotNull { it.character }
        if (newCharacters.isNotEmpty()) {
            val existingMalIds = animeCharacterRepository
                .findAllByMalIdIn(newCharacters.map { it.malId })
                .map { it.malId }
                .toSet()

            val charactersToSave = newCharacters.filterNot {
                it.malId in existingMalIds
            }.map { character ->
                AnimeCharacterTable(
                    malId = character.malId,
                    name = character.name,
                    nameEn = character.nameEn,
                    nameKanji = character.nameKanji,
                    image = character.image,
                    aboutEn = character.aboutEn,
                    aboutRu = character.aboutRu,
                    pictures = character.pictures,
                )
            }

            if (charactersToSave.isNotEmpty()) {
                animeCharacterRepository.saveAllAndFlush(charactersToSave)
            }
        }

        // Затем сохраняем новые роли
        val newRoles = processedData.flatMap { it.roles }
        if (newRoles.isNotEmpty()) {
            // Получаем актуальные ссылки на персонажей из БД
            val characters = animeCharacterRepository
                .findAllByMalIdIn(newRoles.map { it.character.malId })
                .associateBy { it.malId }

            // Проверяем существующие роли
            val existingRoles = animeCharacterRoleRepository
                .findAllByAnimeIdAndCharacterIdIn(
                    newRoles.first().anime.id,
                    characters.values.map { it.id },
                )
                .map { "${it.anime.id}:${it.character.id}" }
                .toSet()

            val rolesToSave = newRoles
                .mapNotNull { role ->
                    val character = characters[role.character.malId] ?: return@mapNotNull null
                    val roleKey = "${role.anime.id}:${character.id}"

                    if (roleKey in existingRoles) {
                        null
                    } else {
                        AnimeCharacterRoleTable(
                            anime = role.anime,
                            character = character, // Используем актуальную ссылку на персонажа
                            role = role.role,
                            roleEn = role.roleEn,
                        )
                    }
                }

            if (rolesToSave.isNotEmpty()) {
                animeCharacterRoleRepository.saveAllAndFlush(rolesToSave)
            }
        }
    }

    private suspend fun processCharacterData(
        anime: AnimeTable,
        characterData: JikanAnimeCharactersDto,
        existingCharacter: AnimeCharacterTable?,
    ): ProcessedCharacterData {
        return if (existingCharacter != null) {
            createRoleForCharacter(anime, existingCharacter, characterData.role)
        } else {
            val newCharacter = createNewCharacter(characterData.character.malId, anime)
            createRoleForCharacter(anime, newCharacter, characterData.role)
        }
    }

    private suspend fun createRoleForCharacter(
        anime: AnimeTable,
        character: AnimeCharacterTable,
        role: String,
    ): ProcessedCharacterData {
        val roleText = translateComponent.translateSingleText("$role role")
            .replace("роль", "")
            .replace("роли", "")
            .replace("ролей", "")
            .replace("ролям", "")
            .dropLast(1)

        val existingRole = animeCharacterRoleRepository.findByAnimeIdAndCharacterId(anime.id, character.id)
        if (existingRole != null) {
            return ProcessedCharacterData(character, emptyList())
        }

        val characterRole = AnimeCharacterRoleTable(
            anime = anime,
            character = character,
            role = roleText,
            roleEn = role,
        )

        return ProcessedCharacterData(character, listOf(characterRole))
    }

    private suspend fun createNewCharacter(characterId: Int, anime: AnimeTable): AnimeCharacterTable {
        val (character, pictures) = coroutineScope {
            val characterDeferred = async { jikanComponent.fetchJikanCharacter(characterId) }
            val picturesDeferred = async { jikanComponent.fetchJikanCharacterPictures(characterId) }
            Pair(characterDeferred.await(), picturesDeferred.await())
        }

        val mainImageUrl = character.data.images.jikanJpg.imageUrl
        val mainImagePath = "images/anime/${CompressAnimeImageType.CharacterImage.path}/${anime.url}/${mdFive(mainImageUrl)}.${CompressAnimeImageType.CharacterImage.imageType.textFormat()}"

        // Параллельная загрузка и сохранение изображений
        val (mainImage, additionalImages) = coroutineScope {
            val mainImageDeferred = async(Dispatchers.IO) {
                imageService.saveFileInSThird(
                    filePath = mainImagePath,
                    data = URL(mainImageUrl).readBytes(),
                    compress = false,
                    newImage = true,
                    type = CompressAnimeImageType.CharacterImage,
                )
            }

            val additionalImagesDeferred = pictures.data.map { image ->
                async(Dispatchers.IO) {
                    imageService.saveFileInSThird(
                        filePath = "images/anime/${CompressAnimeImageType.CharacterImage.path}/${anime.url}/pictures/${mdFive(image.jikanJpg.imageUrl)}.${CompressAnimeImageType.CharacterImage.imageType.textFormat()}",
                        data = URL(image.jikanJpg.imageUrl).readBytes(),
                        compress = false,
                        newImage = true,
                        type = CompressAnimeImageType.CharacterImage,
                    )
                }
            }

            Pair(mainImageDeferred.await(), additionalImagesDeferred.awaitAll())
        }

        val name = translateComponent.translateSingleText("The character of anime is ${character.data.name}")
            .replace("Персонаж аниме - ", "")
            .replace("персонаж аниме - ", "")
            .replace("Персонаж аниме — ", "")
            .replace("персонаж аниме — ", "")
            .replace("Персонаж аниме – ", "")
            .replace("персонаж аниме – ", "")
            .replace("Персонаж аниме ", "")
            .replace("персонаж аниме ", "")
            .replace("Персонаж аниме является ", "")
            .replace("персонаж аниме является ", "")
            .replace("Персонажем аниме является ", "")
            .replace("персонажем аниме является ", "")

        return AnimeCharacterTable(
            malId = characterId,
            name = name,
            nameEn = character.data.name,
            nameKanji = character.data.nameKanji,
            image = mainImage,
            aboutEn = character.data.about,
            aboutRu = character.data.about?.let { translateComponent.translateSingleText(it) },
            pictures = additionalImages.toMutableList(),
        )
    }

    private fun fetchExistingShikimoriIds(): List<Int> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(Int::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)
        criteriaQuery.select(root.get("shikimoriId"))
        return entityManager.createQuery(criteriaQuery).resultList
    }

    private suspend fun processBatch(batch: List<KodikAnimeDto>) = coroutineScope {
        try {
            batch.map { animeTemp ->
                async {
                    try {
                        processData(animeTemp)
                    } catch (e: Exception) {
                        logger.error("Failed to process anime ${animeTemp.shikimoriId}", e)
                        logError(animeTemp.shikimoriId, "PARSER | PROCESS_FAILED", e.message)
                    }
                }
            }.awaitAll()
        } catch (e: Exception) {
            logger.error("Failed to process batch PARSER", e)
        }
    }

    private suspend fun fetchNextPage(nextPageUrl: String): KodikResponseDto<KodikAnimeDto> {
        return try {
            client.get(nextPageUrl) {
                headers { contentType(ContentType.Application.Json) }
            }.body()
        } catch (e: Exception) {
            logger.error("Failed to fetch next page", e)
            throw e
        }
    }

    private suspend fun processData(animeKodik: KodikAnimeDto) = coroutineScope {
        try {
            val shikimori = try {
                shikimoriComponent.fetchAnime(animeKodik.shikimoriId)
            } catch (e: Exception) {
                logger.error("Failed to fetch shikimori data for ID ${animeKodik.shikimoriId}: ${e.message}", e)
                throw e
            }

            val licensors = shikimori?.licensors?.filter { it == "DEEP" || it == "Экспонента" || it == "Вольга" }
            val isLicensed = !licensors.isNullOrEmpty()

            if (!isLicensed) {
                if (shikimori == null) {
                    logger.warn("Skipping anime: No Shikimori data for ID ${animeKodik.shikimoriId}")
                    return@coroutineScope
                }

                val jikan = try {
                    jikanComponent.fetchJikan(animeKodik.shikimoriId)
                } catch (e: Exception) {
                    logger.error("❌ Failed to fetch jikan data: ${e.message}")
                    throw e
                }

                val shikimoriRating = shikimori.score.toDoubleOrNull() ?: 0.0
                val userRatesStats = shikimori.usersRatesStats.sumOf { it.value }
                val blockedByRepository = animeBlockedRepository.findById(shikimori.id).isPresent
                val studioBlocked = shikimori.studios.any {
                    animeBlockedByStudioRepository.findById(it.id).isPresent
                }
                val alreadyExists = animeRepository.findByShikimoriId(shikimori.id).isPresent

                val skippingReasons = mutableListOf<String>()
                if (blockedByRepository) skippingReasons.add("Blocked in repository")
                if (studioBlocked) skippingReasons.add("Studio blocked")
                if (alreadyExists) skippingReasons.add("Already exists")
                if (userRatesStats <= 4000 && !(userRatesStats > 500 && shikimori.status == "ongoing")) {
                    skippingReasons.add("Insufficient user rates")
                }

                if (skippingReasons.isNotEmpty()) {
                    logger.info("Skipping anime ${animeKodik.shikimoriId}: ${skippingReasons.joinToString(", ")}")
                    return@coroutineScope
                }

                if (!animeBlockedRepository.findById(shikimori.id).isPresent &&
                    (userRatesStats > 4000 || (userRatesStats > 500 && shikimori.status == "ongoing")) &&
                    !studioBlocked &&
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

                    val asyncSupervisor = CoroutineScope(coroutineContext + SupervisorJob())

                    val animeIdsDeferred = asyncSupervisor.async {
                        try {
                            haglundComponent.fetchHaglundIds(shikimori.id)
                        } catch (e: Exception) {
                            logger.error("❌ Failed to fetch haglund ids: ${e.message}")
                            throw e
                        }
                    }

                    var urlLinkPath = commonParserComponent.translit(if (!shikimori.russianLic.isNullOrEmpty() && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)

                    if (animeRepository.findByUrl(urlLinkPath).isPresent) {
                        urlLinkPath = "${commonParserComponent.translit(if (shikimori.russianLic != null && commonParserComponent.checkEnglishLetter(shikimori.russian)) shikimori.russianLic else shikimori.russian)}-${airedOn.year}"
                    }

                    val videosShikimoriDeferred = asyncSupervisor.async {
                        try {
                            shikimoriComponent.fetchVideos(shikimori.id)
                        } catch (e: Exception) {
                            logger.error("❌ Failed to fetch videos: ${e.message}")
                            throw e
                        }
                    }

                    val shikimoriScreenshotsDeferred = asyncSupervisor.async {
                        try {
                            shikimoriComponent.fetchScreenshots(shikimori.id)
                        } catch (e: Exception) {
                            logger.error("❌ Failed to fetch screenshots: ${e.message}")
                            throw e
                        }
                    }

                    val shikimoriExternalLinksDeferred = asyncSupervisor.async {
                        try {
                            shikimoriComponent.fetchExternalLinks(shikimori.id)
                        } catch (e: Exception) {
                            logger.error("❌ Failed to fetch external links: ${e.message}")
                            throw e
                        }
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

                    val animeIds = try {
                        animeIdsDeferred.await()
                    } catch (e: Exception) {
                        logger.error("❌ Failed to get anime ids: ${e.message}")
                        logError(animeKodik.shikimoriId, "PARSER | FETCH_HAGLUND_IDS_FAILED", e.message)
                        return@coroutineScope
                    }

                    val imagesDeferred = asyncSupervisor.async {
                        try {
                            fetchImageComponent.fetchAndSaveAnimeImages(shikimori.id, animeIds.kitsu, urlLinkPath)
                        } catch (e: Exception) {
                            logger.error("❌ Failed to fetch and save images: ${e.message}")
                            logError(animeKodik.shikimoriId, "PARSER | IMAGES_FAILED", e.message)
                            throw e
                        }
                    }

                    val (images, bufferedLargeImage) = try {
                        imagesDeferred.await()
                    } catch (e: Exception) {
                        logger.error("❌ Failed to process images: ${e.message}")
                        logError(animeKodik.shikimoriId, "PARSER | IMAGES_FAILED", e.message)
                        return@coroutineScope
                    }

                    val episodesReady = episodesComponent.fetchEpisodes(
                        shikimoriId = shikimori.id,
                        kitsuId = animeIds.kitsu.toString(),
                        type = type,
                        urlLinkPath = urlLinkPath,
                        defaultImage = images.medium,
                    )

                    val translationsCountReady = episodesComponent.translationsCount(episodesReady)
                    val translations = translationsCountReady.map { it.translation }

                    val durations = episodesReady.map { it.duration ?: 0 }
                    val duration = durations.sumOf { it }

                    val videos = try {
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
                    } catch (e: Exception) {
                        logger.error("❌ Failed to process videos: ${e.message}")
                        logError(animeKodik.shikimoriId, "PARSER | VIDEOS_FAILED", e.message)
                        return@coroutineScope
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

                    val screenshots = try {
                        shikimoriScreenshotsDeferred.await().map { screenshot ->
                            fetchImageComponent.saveImage(screenshot, CompressAnimeImageType.Screenshot, urlLinkPath, true)
                        }.toMutableList()
                    } catch (e: Exception) {
                        logger.error("❌ Failed to process screenshots: ${e.message}")
                        logError(animeKodik.shikimoriId, "PARSER | SCREENSHOTS_FAILED", e.message)
                        return@coroutineScope
                    }

                    val externalLinks = try {
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
                    } catch (e: Exception) {
                        logger.error("❌ Failed to process external links: ${e.message}")
                        logError(animeKodik.shikimoriId, "PARSER | EXTERNAL_LINKS_FAILED", e.message)
                        return@coroutineScope
                    }

                    var episodesCount =
                        when {
                            shikimori.episodes == 0 && status == AnimeStatus.Ongoing -> null
                            shikimori.episodes < episodesReady.size -> episodesReady.size
                            else -> shikimori.episodes
                        }

                    if (episodesCount != null) {
                        if (episodesCount < episodesReady.size) {
                            episodesCount = episodesReady.size
                        }
                    }

                    val animeToSave =
                        AnimeTable(
                            type = type,
                            url = urlLinkPath,
                            playerLink = animeKodik.link,
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
                            year = airedOn.year,
                            nextEpisode = nextEpisode,
                            episodesCount = episodesCount,
                            episodesAired = episodesReady.size,
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
                            duration = if (episodesReady.size * 16 < duration && status == AnimeStatus.Released) {
                                duration
                            } else {
                                null
                            },
                            screenshots = screenshots,
                            shikimoriRating = shikimoriRating,
                            shikimoriVotes = userRatesStats,
                            ratingMpa = ratingMpa,
                            minimalAge = minimalAge,
                            season = season,
                            accentColor = commonParserComponent.getMostCommonColor(bufferedLargeImage),
                        )

                    animeToSave.addTranslationCount(translationsCountReady)
                    animeToSave.addTranslation(translations)
                    animeToSave.addEpisodesAll(episodesReady)

//                    animeToSave.nextEpisode?.let { nextEpisodeDate ->
//                        animeToSave.updateEpisodeSchedule(nextEpisodeDate)
//                    }

                    animeToSave.addAllAnimeGenre(genres)
                    animeToSave.addAllAnimeStudios(studios)
                    animeToSave.addVideos(videos)
                    animeToSave.addExternalLinks(externalLinks)

                    val preparationToSaveAnime = animeRepository.findByShikimoriId(shikimori.id)
                    if (preparationToSaveAnime.isPresent) {
                        return@coroutineScope
                    } else {
                        animeRepository.saveAndFlush(animeToSave)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Unexpected error in processData: ${e.message}")
            logError(animeKodik.shikimoriId, "PARSER | processData", e.message)
            return@coroutineScope
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

    private companion object {
        const val BATCH_SIZE = 2
    }

    private suspend fun logError(shikimoriId: Int, cause: String, message: String?) {
        try {
            animeErrorParserRepository.save(
                AnimeErrorParserTable(
                    message = message,
                    cause = cause,
                    shikimoriId = shikimoriId,
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to log error for anime $shikimoriId", e)
        }
    }
}
