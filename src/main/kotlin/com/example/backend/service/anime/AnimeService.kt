package com.example.backend.service.anime


import com.example.backend.jpa.anime.*
import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.models.ServiceResponse
import com.example.backend.models.anime.AnimeBufferedImagesSup
import com.example.backend.models.anime.AnimeImagesTypes
import com.example.backend.models.anime.AnimeMusicType
import com.example.backend.models.animeParser.*
import com.example.backend.models.animeParser.haglund.AnimeIdsHagLund
import com.example.backend.models.animeParser.kitsu.KitsuDefaults
import com.example.backend.models.animeParser.kitsu.KitsuDetails
import com.example.backend.models.animeParser.kitsu.anime.AnimeKitsu
import com.example.backend.models.animeParser.kitsu.episodes.EpisodesKitsu
import com.example.backend.models.animeParser.kodik.AnimeParser
import com.example.backend.models.animeParser.kodik.AnimeTranslations
import com.example.backend.models.animeParser.microsoft.default.TextTranslations
import com.example.backend.models.animeParser.microsoft.request.TextMicRequest
import com.example.backend.models.animeParser.shikimori.AnimeMediaParse
import com.example.backend.models.animeParser.shikimori.RelationParse
import com.example.backend.models.animeParser.shikimori.SimilarParse
import com.example.backend.models.animeResponse.common.RatingResponse
import com.example.backend.models.animeResponse.detail.AnimeDetail
import com.example.backend.models.animeResponse.episode.EpisodeWithLink
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.light.AnimeLightWithType
import com.example.backend.models.animeResponse.media.AnimeMediaResponse
import com.example.backend.models.jikan.Jikan
import com.example.backend.models.jikan.JikanData
import com.example.backend.models.jikan.JikanThemes
import com.example.backend.models.users.StatusFavourite
import com.example.backend.repository.anime.*
import com.example.backend.repository.user.UserRatingCountMangaRepository
import com.example.backend.repository.user.UserRatingCountRepository
import com.example.backend.service.image.ImageService
import com.example.backend.util.*
import com.example.backend.util.common.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import javax.persistence.*
import javax.persistence.criteria.*
import javax.transaction.Transactional
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.ws.rs.NotFoundException


@Service
class AnimeService : AnimeRepositoryImpl {

    @Value("\${anime.ko.token}")
    lateinit var animeToken: String

    @Autowired
    lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    lateinit var imageService: ImageService

    @Autowired
    lateinit var animeGenreRepository: AnimeGenreRepository

    @Autowired
    lateinit var animeTranslationRepository: AnimeTranslationRepository

    @Autowired
    lateinit var animeMediaRepository: AnimeMediaRepository

    @Autowired
    lateinit var animeRepository: AnimeRepository

    @Autowired
    lateinit var animeMusicRepository: AnimeMusicRepository

    @Autowired
    private lateinit var userRatingCountRepository: UserRatingCountRepository

    @Autowired
    private lateinit var userRatingCountMangaRepository: UserRatingCountMangaRepository

    @Autowired
    private lateinit var animeRelatedRepository: AnimeRelatedRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    val coroutineScope = CoroutineScope(Dispatchers.Default)



    val client = HttpClient {
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }


    override fun getAnime(
        pageNum: @Min(value = 0.toLong()) @Max(value = 500.toLong()) Int,
        pageSize: @Min(value = 1.toLong()) @Max(value = 500.toLong()) Int,
        order: String?,
        genres: List<String>?,
        status: String?,
        searchQuery: String?,
        ratingMpa: String?,
        season: String?,
        minimalAge: Int?,
        type: String?,
        year: List<Int>?,
        studio: String?,
        translations: List<String>?
    ): ServiceResponse<AnimeLight> {
        val actualStatus = status?.ifEmpty { null }
        val actualSearch = searchQuery?.ifEmpty { null }

        val cacheKey = "$pageNum|$pageSize|$order|${genres?.size}|$status|${year?.size}|${translations?.size}"

        println("Anime param = $pageNum | $pageSize | $order | ${genres?.size} | $status | ${year?.size} | ${translations?.size}")
        val sort = when (order) {
            "popular" -> Sort.by(
                Sort.Order(Sort.Direction.DESC, "views"),
                Sort.Order(Sort.Direction.DESC, "countRate")
            )

            "random" -> JpaSort.unsafe("random()")

            "views" -> Sort.by(
                Sort.Order(Sort.Direction.DESC, "views")
            )

            else -> null
        }
        val pageable: Pageable = when {
            sort != null -> {
                PageRequest.of(pageNum, pageSize, sort)
            }

            else -> PageRequest.of(pageNum, pageSize)
        }
        return animeLightSuccess(
            listToAnimeLight(
                findAnime(
                    pageable = pageable,
                    status = actualStatus,
                    searchQuery = actualSearch,
                    ratingMpa = ratingMpa,
                    season = season,
                    minimalAge = minimalAge,
                    type = type,
                    year = year,
                    genres = genres,
                    translationIds = translations,
                    studio = studio
                )
            )
        )
    }

    fun findAnime(
        pageable: Pageable,
        status: String?,
        searchQuery: String?,
        ratingMpa: String?,
        season: String?,
        minimalAge: Int?,
        type: String?,
        year: List<Int>?,
        genres: List<String>?,
        studio: String?,
        translationIds: List<String>?
    ): List<AnimeTable> {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeTable> = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)
        criteriaQuery.select(root)

        val predicates: MutableList<Predicate> = mutableListOf()
        if (!status.isNullOrEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get<String>("status"), status))
        }

        if (!ratingMpa.isNullOrEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get<String>("ratingMpa"), ratingMpa))
        }

        if (!season.isNullOrEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get<String>("season"), season))
        }

        if (!type.isNullOrEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get<String>("type"), type))
        }

        if (minimalAge != null) {
            predicates.add(criteriaBuilder.equal(root.get<Int>("minimalAge"), minimalAge))
        }

        if (!year.isNullOrEmpty()) {
            predicates.add(root.get<Int>("year").`in`(year))
        }

        if (!studio.isNullOrEmpty()) {
            val studioTable = animeStudiosRepository.findByStudio(studio)
                .orElseThrow { throw NotFoundException("Studio not found") }
            val studioPredicate = criteriaBuilder.isMember(studioTable, root.get<List<AnimeStudiosTable>>("studios"))
            predicates.add(studioPredicate)
        }

        if (!genres.isNullOrEmpty()) {
            val g = mutableListOf<AnimeGenreTable>()
            genres.forEach {
                g.add(animeGenreRepository.findById(it).get())
            }
            for (genre in g) {
                val genrePredicate = criteriaBuilder.isMember(genre, root.get<List<AnimeGenreTable>>("genres"))
                predicates.add(genrePredicate)
            }
        }

        if (!searchQuery.isNullOrEmpty()) {
            val titleExpression: Expression<Boolean> = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%"
            )

            val exactMatchPredicate: Predicate = criteriaBuilder.equal(root.get<String>("title"), searchQuery)
            val otherTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("otherTitles", JoinType.LEFT)
            val otherTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(otherTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%"
            )

            val enTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("titleEn", JoinType.LEFT)
            val enTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(enTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%"
            )

            val japTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("titleJapan", JoinType.LEFT)
            val japTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(japTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%"
            )

            val synTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("synonyms", JoinType.LEFT)
            val synTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(synTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%"
            )

            predicates.addAll(
                listOf(
                    criteriaBuilder.or(titleExpression, exactMatchPredicate),
                    criteriaBuilder.or(otherTitlesExpression),
                    criteriaBuilder.or(enTitlesExpression),
                    criteriaBuilder.or(japTitlesExpression),
                    criteriaBuilder.or(synTitlesExpression),
                )
            )
        }

        if (!translationIds.isNullOrEmpty()) {
            val translationJoin = root.join<AnimeTable, AnimeTranslationTable>("translation")
            val translationIdsPredicate = criteriaBuilder.isTrue(
                translationJoin.get<AnimeTranslationTable>("id").`in`(translationIds.mapNotNull { it.toIntOrNull() })
            )
            predicates.add(translationIdsPredicate)
        }

        if (predicates.isNotEmpty()) {
            if (searchQuery == null) {
                criteriaQuery.distinct(true).where(criteriaBuilder.and(*predicates.toTypedArray()))
            } else
                criteriaQuery.distinct(true).where(criteriaBuilder.or(*predicates.toTypedArray()))
        }

        val query = entityManager.createQuery(criteriaQuery)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return query.resultList
    }

    override fun getAnimeUsersStatusCount(url: String): MutableMap<StatusFavourite, Long> {
        checkAnime(url)

        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(Tuple::class.java)
        val animeRoot = criteriaQuery.from(AnimeTable::class.java)
        val userFavoriteJoin: Join<AnimeTable, UserFavoriteAnime> = animeRoot.join("favorites", JoinType.LEFT)

        criteriaQuery.select(
            criteriaBuilder.tuple(
                userFavoriteJoin.get<StatusFavourite>("status"),
                criteriaBuilder.count(userFavoriteJoin)
            )
        )
        criteriaQuery.where(criteriaBuilder.equal(animeRoot.get<String>("url"), url))
        criteriaQuery.groupBy(userFavoriteJoin.get<StatusFavourite>("status"))

        val result = entityManager.createQuery(criteriaQuery).resultList

        val statusList = StatusFavourite.values()
        val resultMap = result.associate {
            val status = it.get(0, StatusFavourite::class.java)
            val count = it.get(1, Long::class.java)
            status to count
        }.toMutableMap()

        if (resultMap.values.all { it == 0L }) {
            return mutableMapOf()
        }

        statusList.forEach {
            resultMap.putIfAbsent(it, 0L)
        }

        return resultMap
    }

    override fun getAnimeById(id: String): ServiceResponse<AnimeDetail> {
        return try {
            try {
                val anime = animeRepository.findDetails(id).get()
                return ServiceResponse(
                    data = listOf(animeToAnimeDetail(anime)),
                    message = "Success",
                    status = HttpStatus.OK
                )
            } catch (e: Exception) {
                ServiceResponse(
                    data = null,
                    message = "Anime with id = $id not found",
                    status = HttpStatus.NOT_FOUND
                )
            }
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    override fun getAnimeRelated(url: String): ServiceResponse<AnimeLightWithType> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("related", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime: AnimeTable? = entityManager.createQuery(criteriaQuery).singleResult

        if (anime != null) {
            val relatedAnimeList: List<AnimeLightWithType> = anime.related.mapNotNull { related ->
                val relatedCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
                val relatedRoot = relatedCriteriaQuery.from(AnimeTable::class.java)
                relatedCriteriaQuery.select(relatedRoot)
                    .where(criteriaBuilder.equal(relatedRoot.get<Int>("shikimoriId"), related.shikimoriId))

                val relatedAnimeList: List<AnimeTable> = entityManager.createQuery(relatedCriteriaQuery).resultList

                if (relatedAnimeList.isNotEmpty()) {
                    val relatedAnime: AnimeTable = relatedAnimeList.first()
                    val animeLight =
                        animeTableToAnimeLight(relatedAnime) // Функция для создания объекта AnimeLight из AnimeTable
                    AnimeLightWithType(animeLight, relatedToLight(related))
                } else {
                    null
                }
            }

            return if (relatedAnimeList.isNotEmpty()) {
                ServiceResponse(
                    data = relatedAnimeList,
                    message = "Success",
                    status = HttpStatus.OK
                )
            } else {
                ServiceResponse(
                    data = null,
                    message = "Success",
                    status = HttpStatus.OK
                )
            }
        } else {
            return ServiceResponse(
                data = null,
                message = "Anime with url = $url not found",
                status = HttpStatus.NOT_FOUND
            )
        }
    }


    override fun getAnimeSimilar(url: String): ServiceResponse<AnimeLight> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("similarAnime", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime: AnimeTable? = entityManager.createQuery(criteriaQuery).singleResult

        if (anime != null) {
            val similarCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
            val similarRoot = similarCriteriaQuery.from(AnimeTable::class.java)
            similarCriteriaQuery.select(similarRoot)
                .where(similarRoot.get<Int>("shikimoriId").`in`(anime.similarAnime))

            val similarEntityList = entityManager.createQuery(similarCriteriaQuery).resultList as List<AnimeTable>

            val similarEntityMap: Map<Int, AnimeTable> = similarEntityList.associateBy { it.shikimoriId }

            val similarAnimeList: List<AnimeTable> = anime.similarAnime.mapNotNull { similarAnimeId ->
                similarEntityMap[similarAnimeId]
            }

            return if (similarAnimeList.isNotEmpty()) {
                animeLightSuccess(listToAnimeLight(similarAnimeList))
            } else ServiceResponse(
                data = null,
                message = "Success",
                status = HttpStatus.OK
            )
        } else return ServiceResponse(
            data = null,
            message = "Anime with url = $url not found",
            status = HttpStatus.NOT_FOUND
        )
    }

    override fun getAnimeRating(url: String): List<RatingResponse> {
        val anime = checkAnime(url)

        return userRatingCountRepository.findByAnime(anime).map { ratingCount ->
            RatingResponse(ratingCount.rating, ratingCount.count)
        }
    }


    override fun getAnimeScreenshotsById(id: String): ServiceResponse<String> {
        return try {
            try {
                val criteriaBuilder = entityManager.criteriaBuilder
                val criteriaQuery = criteriaBuilder.createQuery(String::class.java)
                val root = criteriaQuery.from(AnimeTable::class.java)
                val screenshotsPath: Join<AnimeTable, String> = root.joinList("screenshots")

                criteriaQuery.select(screenshotsPath)
                    .where(criteriaBuilder.equal(root.get<String>("url"), id))
                    .distinct(true)

                val query = entityManager.createQuery(criteriaQuery)

                ServiceResponse(
                    data = query.resultList,
                    message = "Success",
                    status = HttpStatus.OK
                )
            } catch (e: Exception) {
                ServiceResponse(
                    data = null,
                    message = "Anime with id = $id not found",
                    status = HttpStatus.NOT_FOUND
                )
            }
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    fun getMostCommonColor(image: BufferedImage): String {
        val brightestColors = getBrightestColors(image)
        val numColors = brightestColors.size

        if (numColors == 0) {
            return "#FFFFFF" // Белый цвет
        }

        val red = brightestColors.sumOf { it.red } / numColors
        val green = brightestColors.sumOf { it.green } / numColors
        val blue = brightestColors.sumOf { it.blue } / numColors

        return String.format("#%02X%02X%02X", red, green, blue)
    }

    fun getBrightestColors(image: BufferedImage): List<Color> {
        // Создаем объект Map для хранения цветов и их количества
        val colorCount = mutableMapOf<Int, Int>()

        // Перебираем каждый пиксель на изображении и сохраняем его цвет в списке
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                val red = (pixel shr 16 and 0xFF) / 255.0
                val green = (pixel shr 8 and 0xFF) / 255.0
                val blue = (pixel and 0xFF) / 255.0

                // Исключаем цвета, близкие к белому и черному
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

        // Находим ключи цветов с количеством больше 50
        val mostCommonPixels = colorCount.filter { it.value > 30 }.keys

        // Сортируем цвета по яркости
        val brightestColors = mostCommonPixels.map { Color(it) }.sortedByDescending { colorBrightness(it) }

        return brightestColors
    }

    fun colorBrightness(color: Color): Double {
        return (color.red * 299 + color.green * 587 + color.blue * 114) / 1000.0
    }

    override fun getAnimeMediaById(id: String): ServiceResponse<AnimeMediaResponse> {
        return try {
            try {
                val criteriaBuilder = entityManager.criteriaBuilder
                val criteriaQuery = criteriaBuilder.createQuery(AnimeMediaTable::class.java)
                val root = criteriaQuery.from(AnimeTable::class.java)

                val joinAnimeTable: Join<AnimeTable, AnimeMediaTable> = root.joinSet("media", JoinType.LEFT)

                criteriaQuery.select(joinAnimeTable)
                    .where(criteriaBuilder.equal(root.get<String>("url"), id))

                val query = entityManager.createQuery(criteriaQuery)

                val mediaList = query.resultList.map { animeMedia ->
                    AnimeMediaResponse.fromAnimeMediaTable(animeMedia)
                }

                ServiceResponse(
                    data = mediaList,
                    message = "Success",
                    status = HttpStatus.OK
                )
            } catch (e: Exception) {
                ServiceResponse(
                    data = null,
                    message = "Anime with id = $id not found",
                    status = HttpStatus.NOT_FOUND
                )
            }
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    override fun getAnimeGenres(): ServiceResponse<AnimeGenreTable> {
        return try {
            ServiceResponse(
                data = animeGenreRepository.findAll(),
                message = "Success",
                status = HttpStatus.OK
            )
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    override fun getAnimeYears(): ServiceResponse<String> {
        return try {
            ServiceResponse(
                data = animeRepository.findDistinctByYear(),
                message = "Success",
                status = HttpStatus.OK
            )
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    override fun getAnimeStudios(): ServiceResponse<AnimeStudiosTable> {
        return try {
            ServiceResponse(
                data = animeStudiosRepository.findAll(),
                message = "Success",
                status = HttpStatus.OK
            )
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }

    override fun getAnimeTranslations(): ServiceResponse<AnimeTranslationTable> {
        return try {
            ServiceResponse(
                data = animeTranslationRepository.findAll(),
                message = "Success",
                status = HttpStatus.OK
            )
        } catch (e: Exception) {
            ServiceResponse(
                data = null,
                message = "Error: ${e.message}",
                status = HttpStatus.BAD_REQUEST
            )
        }
    }


    fun animeLightSuccess(
        animeLight: List<AnimeLight>
    ): ServiceResponse<AnimeLight> {
        return ServiceResponse(
            data = animeLight,
            message = "Success",
            status = HttpStatus.OK
        )
    }

    fun animeToAnimeDetail(
        anime: AnimeTable
    ): AnimeDetail {
        return AnimeDetail(
            url = anime.url,
            title = anime.title,
            image = AnimeImagesTypes(large = anime.images.large, medium = anime.images.medium, cover = anime.images.cover),
            studio = anime.studios.toList(),
            season = anime.season,
            description = anime.description,
            otherTitles = anime.otherTitles.distinct(),
            shikimoriRating = anime.shikimoriRating,
            nextEpisode = anime.nextEpisode,
            year = anime.year,
            releasedAt = anime.releasedAt,
            airedAt = anime.airedAt,
            type = anime.type,
            rating = anime.totalRating,
            episodesCount = anime.episodesCount,
            episodesCountAired = anime.episodesAires,
            linkPlayer = anime.link,
            genres = anime.genres.toList(),
            status = anime.status,
            ratingMpa = anime.ratingMpa,
            minimalAge = anime.minimalAge
        )
    }

    override fun getAnimeEpisodesWithPaging(url: String, pageNumber: Int, pageSize: Int, sort: String?): EpisodeWithLink {
        checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime: AnimeTable? = entityManager.createQuery(criteriaQuery).singleResult

        if (anime != null) {
            val temp = when(sort) {
                "numberAsc" -> anime.episodes.toList().sortedBy { it.number }
                "numberDesc" -> anime.episodes.toList().sortedByDescending { it.number }
                else -> anime.episodes.toList()
            }

            val episodes = temp.toPage(PageRequest.of(pageNumber, pageSize)).content

            return EpisodeWithLink(
                link = anime.link,
                episodes = episodeToEpisodeLight(episodes)
            )
        }

        return EpisodeWithLink()
    }


    override fun getAnimeEpisodeByNumberAndAnime(url: String, number: Int): AnimeEpisodeTable {
        checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime: AnimeTable? = entityManager.createQuery(criteriaQuery).singleResult

        val episode = anime?.episodes?.find { it.number == number }

        if(episode != null) return episode

        throw NotFoundException("Anime episode not found")
    }

    override fun addTranslationsToDB(transltionsIDs: List<Int>) {
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
            }.body<AnimeResponse<AnimeTranslations>>()
        }
        transltionsIDs.forEach { translation ->
            val t = translations.result.find { it.id == translation }
            if (t != null) {
                checkKodikTranslation(t.id, t.title, "voice")
            }
        }

    }

    @Transactional
    override fun addDataToDB(translationID: String) {
        var nextPage: String? = "1"

        var ar = runBlocking {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "kodikapi.com/list"
                }
                parameter("token", animeToken)
                parameter("limit", 100)
                parameter("sort", "shikimori_rating")
                parameter("order", "desc")
                parameter("types", "anime-serial")
                parameter("camrip", false)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
                parameter(
                    "anime_genres",
                    "безумие, боевые искусства, вампиры, военное, гарем, демоны, детектив, детское, дзёсей, драма, игры, исторический, комедия, космос, машины, меха, музыка, пародия, повседневность, полиция, приключения, психологическое, романтика, самураи, сверхъестественное, спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер, ужасы, фантастика, фэнтези, школа, экшен"
                )
                parameter("translation_id", translationID)
            }.body<AnimeResponse<AnimeParser>>()
        }

        while (nextPage != null) {
            ar.result.forEach Loop@{ animeTemp ->
                try {
                    println(animeTemp.title)
                    val anime = runBlocking {
                        client.get {
                            headers {
                                contentType(ContentType.Application.Json)
                            }
                            url {
                                protocol = URLProtocol.HTTPS
                                host = "kodikapi.com/search"
                            }

                            parameter("token", animeToken)
                            parameter("with_material_data", true)
                            parameter("full_match", true)
                            parameter("title_orig", animeTemp.title)
                            parameter("sort", "shikimori_rating")
                            parameter("order", "desc")
                            parameter("types", "anime-serial")
                            parameter("camrip", false)
                            parameter("with_episodes_data", true)
                            parameter("not_blocked_in", "ALL")
                            parameter("with_material_data", true)
                            parameter(
                                "anime_genres",
                                "безумие, боевые искусства, вампиры, военное, гарем, демоны, детектив, детское, дзёсей, драма, игры, исторический, комедия, космос, машины, меха, музыка, пародия, повседневность, полиция, приключения, психологическое, романтика, самураи, сверхъестественное, спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер, ужасы, фантастика, фэнтези, школа, экшен"
                            )
                            parameter("translation_id", translationID)
                        }.body<AnimeResponse<AnimeParser>>()
                    }.result[0]

                    if (
                        !anime.materialData.title.contains("Атака Титанов") &&
                        !anime.materialData.title.contains("Атака титанов") &&
                        anime.shikimoriId.toInt() != 226 &&
                        anime.shikimoriId.toInt() != 37517 &&
                        anime.shikimoriId.toInt() != 1535 &&
                        anime.shikimoriId.toInt() != 34542 &&
                        anime.shikimoriId.toInt() != 22319 &&
                        anime.shikimoriId.toInt() != 7088 &&
                        anime.shikimoriId.toInt() != 10465 &&
                        anime.shikimoriId.toInt() != 8577 &&
                        anime.shikimoriId.toInt() != 40010 &&
                        anime.shikimoriId.toInt() != 6987 &&
                        anime.shikimoriId.toInt() != 30831 &&
                        anime.shikimoriId.toInt() != 38040 &&
                        anime.shikimoriId.toInt() != 38924 &&
                        anime.shikimoriId.toInt() != 6201 &&
                        anime.shikimoriId.toInt() != 17729 &&
                        anime.shikimoriId.toInt() != 19429 &&
                        anime.shikimoriId.toInt() != 24833 &&
                        anime.shikimoriId.toInt() != 35241 &&
                        anime.shikimoriId.toInt() != 37998 &&
                        anime.shikimoriId.toInt() != 34177 &&
                        anime.shikimoriId.toInt() != 34019 &&
                        anime.shikimoriId.toInt() != 39469 &&
                        anime.shikimoriId.toInt() != 36632
                    ) {
                        val tempingAnime = animeRepository.findByShikimoriId(anime.shikimoriId.toInt())

                        if (!tempingAnime.isPresent) {
                            val startTime = System.currentTimeMillis()

                            val g = mutableListOf<AnimeGenreTable>()
                            anime.materialData.genres.forEach { genre ->
                                val genreIs = animeGenreRepository.findByGenre(genre).isPresent
                                if (genreIs) {
                                    val temp = animeGenreRepository.findByGenre(genre).get()
                                    g.add(
                                        AnimeGenreTable(id = temp.id, genre = temp.genre)
                                    )
                                } else {
                                    if (genre == "яой" || genre == "эротика" || genre == "хентай" || genre == "Яой" || genre == "Хентай" || genre == "Эротика") {
                                        return@Loop
                                    }
                                    animeGenreRepository.save(
                                        AnimeGenreTable(genre = genre)
                                    )
                                    g.add(
                                        animeGenreRepository.findByGenre(genre = genre).get()
                                    )
                                }
                            }
                            val st = mutableListOf<AnimeStudiosTable>()
                            anime.materialData.animeStudios.forEach { studio ->
                                val studioIs = animeStudiosRepository.findByStudio(studio).isPresent
                                if (studioIs) {
                                    val temp = animeStudiosRepository.findByStudio(studio).get()
                                    st.add(
                                        AnimeStudiosTable(id = temp.id, studio = temp.studio)
                                    )
                                } else {
                                    animeStudiosRepository.save(
                                        AnimeStudiosTable(studio = studio)
                                    )
                                    st.add(
                                        animeStudiosRepository.findByStudio(studio = studio).get()
                                    )
                                }
                            }

                            val animeIds = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "arm.haglund.dev/api/v2/ids"
                                    }
                                    parameter("source", "myanimelist")
                                    parameter("id", anime.shikimoriId)
                                }.body<AnimeIdsHagLund>()
                            }

                            val mediaDeferred = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = "shikimori.me/api/animes/${anime.shikimoriId}"
                                        }
                                    }.body<AnimeMediaParse>()
                                }.getOrElse {
                                    null
                                }
                            }

                            var mediaTemp: AnimeMediaParse

                            runBlocking {
                                mediaTemp = mediaDeferred.await()!!
                            }

                            val urlLinking = translit(mediaTemp.russian)

                            val relationIdsDeferred = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = "shikimori.me/api/animes/${anime.shikimoriId}/related"
                                        }
                                    }.body<List<RelationParse>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            val relationIds = mutableListOf<RelationParse>()

                            runBlocking {
                                relationIdsDeferred.await()?.let { relationIds.addAll(it) }
                            }


                            val r = mutableListOf<AnimeRelatedTable>()

                            relationIds.take(30).forEach {
                                r.add(
                                    if (it.anime == null) {
                                        animeRelatedRepository.save(
                                            AnimeRelatedTable(
                                                type = it.relationRussian.toString(),
                                                shikimoriId = it.manga!!.id,
                                                typeEn = it.relation.toString()
                                            )
                                        )
                                    } else {
                                        animeRelatedRepository.save(
                                            AnimeRelatedTable(
                                                type = it.relationRussian.toString(),
                                                shikimoriId = it.anime.id,
                                                typeEn = it.relation.toString()
                                            )
                                        )
                                    }
                                )
                            }

                            val similarIdsDeferred = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        headers {
                                            contentType(ContentType.Application.Json)
                                        }
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = "shikimori.me/api/animes/${anime.shikimoriId}/similar"
                                        }
                                    }.body<List<SimilarParse>>().flatMap { similar ->
                                        listOfNotNull(similar.id)
                                    }.map { it }
                                }.getOrElse {
                                    null
                                }
                            }

                            val media = mediaTemp.videos.map { video ->
                                if (video.hosting != "vk") {
                                    animeMediaRepository.save(
                                        AnimeMediaTable(
                                            url = video.url,
                                            imageUrl = video.imageUrl,
                                            playerUrl = video.playerUrl,
                                            name = video.name,
                                            kind = video.kind,
                                            hosting = video.hosting
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            var animeImages: AnimeImagesTypes? = null

                            var image: AnimeBufferedImagesSup? = null

                            val kitsuAnime = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    client.get {
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = "kitsu.io"
                                            encodedPath = "/api/edge/anime/${animeIds.kitsu}"
                                        }
                                        header("Accept", "application/vnd.api+json")
                                    }.body<KitsuDetails<AnimeKitsu>>()
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
                                            host = "${Constants.Jikan}${Constants.jikanAnime}${anime.shikimoriId}"
                                        }
                                    }.body<Jikan<JikanData>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            runBlocking {
                                val kitsuData = kitsuAnime.await()?.data
                                val jikanData = jikanImage.await()?.data
                                if (kitsuData != null) {
                                    animeImages = AnimeImagesTypes(
                                        large = imageService.saveFileInSThird(
                                            "images/large/$urlLinking.png",
                                            URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                            compress = true,
                                            width = 400,
                                            height = 640
                                        ),
                                        medium = imageService.saveFileInSThird(
                                            "images/medium/$urlLinking.png",
                                            URL(kitsuData.attributesKitsu.posterImage.original).readBytes(),
                                            compress = true,
                                            width = 200,
                                            height = 440
                                        ),
                                        cover = if (kitsuData.attributesKitsu.coverImage.coverLarge != null)
                                            imageService.saveFileInSThird(
                                                "images/cover/$urlLinking.png",
                                                URL(kitsuData.attributesKitsu.coverImage.coverLarge).readBytes(),
                                                compress = true,
                                                width = 800,
                                                height = 200
                                            )
                                        else null,
                                    )
                                    image = AnimeBufferedImagesSup(
                                        large = ImageIO.read(URL(kitsuData.attributesKitsu.posterImage.original)),
                                        medium = ImageIO.read(URL(kitsuData.attributesKitsu.posterImage.large)),
                                    )
                                } else if (jikanData != null) {
                                    animeImages = AnimeImagesTypes(
                                        large = imageService.saveFileInSThird(
                                            "images/large/$urlLinking.png",
                                            URL(jikanData.images.jikanJpg.largeImageUrl).readBytes()
                                        ),
                                        medium = imageService.saveFileInSThird(
                                            "images/medium/$urlLinking.png",
                                            URL(jikanData.images.jikanJpg.mediumImageUrl).readBytes()
                                        )
                                    )
                                    image = AnimeBufferedImagesSup(
                                        large = ImageIO.read(URL(jikanData.images.jikanJpg.largeImageUrl)),
                                        medium = ImageIO.read(URL(jikanData.images.jikanJpg.mediumImageUrl)),
                                    )
                                }
                            }

                            val episodesReady = mutableListOf<AnimeEpisodeTable>()

                            anime.seasons.forEach { kodikSeason ->
                                if (kodikSeason.key != "0") {
                                    val kitsuEpisodes = mutableListOf<EpisodesKitsu>()
                                    var responseKitsuEpisodes = runBlocking {
                                        client.get {
                                            url {
                                                protocol = URLProtocol.HTTPS
                                                host = "kitsu.io"
                                                encodedPath = "/api/edge/anime/${animeIds.kitsu}/episodes"
                                            }
                                            header("Accept", "application/vnd.api+json")
                                            parameter("page%5Boffset%5D", 0)
                                            parameter("page%5Blimit%5D", 20)
                                            parameter("sort", "number")
                                        }.body<KitsuDefaults<EpisodesKitsu>>()
                                    }

                                    while (responseKitsuEpisodes.data != null) {
                                        kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
                                        responseKitsuEpisodes =
                                            if (responseKitsuEpisodes.links.next != null) runBlocking {
                                                delay(1000)
                                                client.get {
                                                    url {
                                                        protocol = URLProtocol.HTTPS
                                                        host = "kitsu.io"
                                                        encodedPath =
                                                            responseKitsuEpisodes.links.next?.replace(
                                                                "https://kitsu.io",
                                                                ""
                                                            ).toString()
                                                    }
                                                    header("Accept", "application/vnd.api+json")
                                                }.body<KitsuDefaults<EpisodesKitsu>>()
                                            } else KitsuDefaults()
                                    }

                                    episodesReady.addAll(runBlocking {
                                        processEpisodes(
                                            anime.shikimoriId,
                                            anime.link,
                                            urlLinking,
                                            kodikSeason.value.episodes,
                                            kitsuEpisodes,
                                            animeImages!!.medium
                                        )
                                    })
                                }
                            }

                            val jikanThemesDefered = CoroutineScope(Dispatchers.Default).async {
                                delay(1000)
                                runCatching {
                                    client.get {
                                        headers {
                                            contentType(ContentType.Application.Json)
                                        }
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host =
                                                "${Constants.Jikan}${Constants.jikanAnime}${anime.shikimoriId}/themes"
                                        }
                                    }.body<Jikan<JikanThemes>>()
                                }.getOrElse {
                                    null
                                }
                            }

                            val music: MutableList<AnimeMusicTable> = mutableListOf()

                            CoroutineScope(Dispatchers.Default).launch {
                                val jikanData = jikanThemesDefered.await()?.data

                                if (jikanData != null) {
                                    jikanData.endings.forEach { ending ->
                                        if (ending != null) {
                                            val endingNormalize = jikanThemesNormalize(ending)
                                            music.add(
                                                animeMusicRepository.save(
                                                    AnimeMusicTable(
                                                        url = "https://music.youtube.com/search?q=$endingNormalize",
                                                        name = endingNormalize,
                                                        episodes = mergeEpisodes(ending),
                                                        type = AnimeMusicType.Ending,
                                                        hosting = "YoutubeMusic"
                                                    )
                                                )
                                            )
                                        }
                                    }
                                    jikanData.openings.forEach { opening ->
                                        if (opening != null) {
                                            val openingNormalize = jikanThemesNormalize(opening)
                                            music.add(
                                                animeMusicRepository.save(
                                                    AnimeMusicTable(
                                                        url = "https://music.youtube.com/search?q=$openingNormalize",
                                                        name = openingNormalize,
                                                        episodes = mergeEpisodes(opening),
                                                        type = AnimeMusicType.Opening,
                                                        hosting = "YoutubeMusic"
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            val screenshotsDeferred = CoroutineScope(Dispatchers.Default).async {
                                runCatching {
                                    client.get {
                                        headers {
                                            contentType(ContentType.Application.Json)
                                        }
                                        url {
                                            protocol = URLProtocol.HTTPS
                                            host = "shikimori.me/api/animes/${anime.shikimoriId}/screenshots"
                                        }
                                    }.body<List<ScreenshotsParse>>().map { screenshot ->
                                        "https://shikimori.me${screenshot.original}"
                                    }
                                }.getOrElse {
                                    null
                                }
                            }
                            val screenShots = mutableListOf<String>()
                            CoroutineScope(Dispatchers.Default).launch {
                                screenShots.addAll(screenshotsDeferred.await()?.toMutableList() ?: mutableListOf())
                            }

                            val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                .withZone(ZoneId.of("Europe/Moscow"))

                            val similarIds = mutableListOf<Int>()

                            CoroutineScope(Dispatchers.Default).launch {
                                val similarData = similarIdsDeferred.await()
                                if (similarData != null) {
                                    similarIds.addAll(similarData)
                                }
                            }

                            val otherTitles = anime.materialData.otherTitles.toMutableList()

                            otherTitles.add(mediaTemp.russianLic ?: mediaTemp.russian)

                            val f = mediaTemp.russianLic
                            val zx = mediaTemp.russian

                            val translations = mutableListOf<AnimeTranslationTable>()

                            episodesReady.forEach { episode ->
                                episode.translations.forEach { translation ->
                                    translations.add(translation)
                                }
                            }


                            val a = AnimeTable(
                                title = if (f != null && !checkEnglishLetter(f)) f else zx,
                                url = urlLinking,
                                ids = AnimeIds(
                                    aniDb = animeIds.aniDb,
                                    aniList = animeIds.aniList,
                                    animePlanet = animeIds.animePlanet,
                                    imdb = animeIds.imdb,
                                    kitsu = animeIds.kitsu,
                                    liveChart = animeIds.liveChart,
                                    notifyMoe = animeIds.notifyMoe,
                                    thetvdb = animeIds.theMovieDb,
                                    myAnimeList = animeIds.myAnimeList
                                ),
                                nextEpisode = if (mediaTemp.nextEpisodeAt != null) {
                                    LocalDateTime.parse(mediaTemp.nextEpisodeAt, formatterUpdated)
                                } else {
                                    null
                                },
                                images = AnimeImages(
                                    large = animeImages?.large ?: "",
                                    medium = animeImages?.medium ?: "",
                                    cover = animeImages?.cover ?: ""
                                ),
                                titleEn = mediaTemp.english.toMutableList(),
                                titleJapan = mediaTemp.japanese.toMutableList(),
                                synonyms = mediaTemp.synonyms.toMutableList(),
                                otherTitles = otherTitles,
                                similarAnime = similarIds.take(30).toMutableList(),
                                related = r.toMutableSet(),
                                status = mediaTemp.status,
                                description = mediaTemp.description.replace(Regex("\\[\\/?[a-z]+.*?\\]"), ""),
                                year = if (mediaTemp.airedAt != null) LocalDate.parse(mediaTemp.airedAt).year else anime.materialData.year,
                                createdAt = anime.createdAt,
                                link = anime.link,
                                airedAt = if (mediaTemp.airedAt != null) LocalDate.parse(mediaTemp.airedAt) else anime.materialData.airedAt,
                                releasedAt = if (mediaTemp.releasedAt != null) LocalDate.parse(mediaTemp.releasedAt) else anime.materialData.releasedAt,
                                episodesCount = mediaTemp.episodes,
                                episodesAires = if (mediaTemp.status == "released") mediaTemp.episodes else mediaTemp.episodesAired,
                                type = anime.materialData.animeType,
                                updatedAt = anime.updatedAt,
                                minimalAge = anime.materialData.minimalAge,
                                screenshots = screenShots,
                                ratingMpa = when (anime.materialData.ratingMpa) {
                                    "PG" -> "PG"
                                    "Rx" -> "R+"
                                    "R+" -> "R+"
                                    "PG-13" -> "PG-13"
                                    "pg" -> "PG"
                                    "R" -> "R"
                                    "pg13" -> "PG-13"
                                    "G" -> "G"
                                    else -> ""
                                },
                                shikimoriId = anime.shikimoriId.toInt(),
                                shikimoriRating = anime.materialData.shikimoriRating,
                                shikimoriVotes = anime.materialData.shikimoriVotes,
                                season = when (anime.materialData.airedAt.month.value) {
                                    12, 1, 2 -> "Winter"
                                    3, 4, 5 -> "Spring"
                                    6, 7, 8 -> "Summer"
                                    else -> "Fall"
                                },
                                accentColor = getMostCommonColor(image?.large!!)
                            )
                            a.addTranslation(translations)
                            a.addEpisodesAll(episodesReady)
                            a.addAllMusic(music)
                            a.addAllAnimeGenre(g)
                            a.addAllAnimeStudios(st)
                            a.addMediaAll(media.filterNotNull())
                            animeRepository.save(a)
                            val endTime = System.currentTimeMillis()
                            val executionTime = endTime - startTime
                            println("Время выполнения запроса: ${executionTime} мс")
                        } else {
//                            val a = animeRepository.findByShikimoriIdWithTranslation(anime.shikimoriId.toInt()).get()
                        }
                    }
                } catch (e: Exception) {
                    return
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


    fun mergeEpisodes(input: String): String {
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


    // Главная функция для обработки всех эпизодов
    suspend fun processEpisodes(shikimoriId: String, playerLink: String, urlLinking: String, kodikEpisodes: Map<String, Episode>, kitsuEpisodes: List<EpisodesKitsu>, imageDefault: String): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()

        val kitsuEpisodesMapped = mutableMapOf<String, EpisodesKitsu?>()
        val translatedTitleMapped = mutableMapOf<String, String>()
        val translatedDescriptionMapped = mutableMapOf<String, String>()
        val tempTranslatedTitle = mutableListOf<TextMicRequest>()
        val tempTranslatedDescription = mutableListOf<TextMicRequest>()

        if(kitsuEpisodes.size >= kodikEpisodes.size) {
            kitsuEpisodes.map { episode ->
                val number = episode.attributes?.number!!
                val kitsuEpisode = findEpisodeByNumber(number, kitsuEpisodes)
                kitsuEpisodesMapped[number.toString()] = kitsuEpisode
                translatedTitleMapped[number.toString()] = kitsuEpisode?.attributes?.titles?.enToUs ?: kitsuEpisode?.attributes?.titles?.en ?: ""
                translatedDescriptionMapped[number.toString()] = kitsuEpisode?.attributes?.description ?: ""
            }
        } else {
            kodikEpisodes.map { (episodeKey, episode) ->
                val kitsuEpisode = findEpisodeByNumber(episodeKey.toInt(), kitsuEpisodes)
                kitsuEpisodesMapped[episodeKey] = kitsuEpisode
                translatedTitleMapped[episodeKey] = kitsuEpisode?.attributes?.titles?.enToUs ?: kitsuEpisode?.attributes?.titles?.en ?: ""
                translatedDescriptionMapped[episodeKey] = kitsuEpisode?.attributes?.description ?: ""
            }
        }

        translatedTitleMapped.map { (episodeKey, title) ->
            tempTranslatedTitle.add(TextMicRequest(title))
        }

        translatedDescriptionMapped.map { (episodeKey, description) ->
            tempTranslatedDescription.add(TextMicRequest(description))
        }

        val a  = if(tempTranslatedTitle.size < 61) {
            translateText(tempTranslatedTitle)
        } else {
            val tempList = mutableListOf<String>()
            val temp = tempTranslatedTitle.chunked(60)
            temp.forEach {
                tempList.addAll(translateText(it))
            }
            tempList
        }

        val b = if(tempTranslatedDescription.size < 61) {
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
            val episodeKeyList = episodeKey.toInt() - 1
            translatedTitleMapped[episodeKey] = a[episodeKeyList]
        }

        translatedDescriptionMapped.map { (episodeKey, title) ->
            translatedDescriptionMapped[episodeKey] = b[(episodeKey.toInt() - 1)]
        }

        val jobs = kodikEpisodes.map { (episodeKey, episode) ->
            coroutineScope.async {
                processEpisode(shikimoriId, playerLink, urlLinking, episodeKey.toInt(), episode.link,
                    kitsuEpisodesMapped[episodeKey], translatedTitleMapped[episodeKey], translatedDescriptionMapped[episodeKey], imageDefault)
            }
        }

        val processedEpisodes = jobs.awaitAll()
        val sortedEpisodes = processedEpisodes.sortedBy { it.number }
        val animeVariations = runBlocking {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "kodikapi.com/search"
                }

                parameter("token", animeToken)
                parameter("with_material_data", true)
                parameter("types", "anime-serial")
                parameter("camrip", false)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
                parameter("shikimori_id", shikimoriId)
                parameter(
                    "anime_genres",
                    "безумие, боевые искусства, вампиры, военное, гарем, демоны, детектив, детское, дзёсей, драма, игры, исторический, комедия, космос, машины, меха, музыка, пародия, повседневность, полиция, приключения, психологическое, романтика, самураи, сверхъестественное, спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер, ужасы, фантастика, фэнтези, школа, экшен"
                )
                parameter("translation_id", "610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002")
            }.body<AnimeResponse<AnimeParser>>()
        }

        animeVariations.result.forEach { anime ->
            sortedEpisodes.forEach { episode ->
                if(episode.number <= anime.lastEpisode) {
                    episode.addTranslation(animeTranslationRepository.findById(anime.translation.id).get())
                }
            }
        }
        episodeReady.addAll(processedEpisodes)

        return episodeReady
    }

    suspend fun processEpisode(
        shikimoriId: String,
        playerLink: String,
        urlLinking: String,
        episode: Int,
        link: String,
        kitsuEpisode: EpisodesKitsu?,
        titleRu: String?,
        descriptionRu: String?,
        imageDefault: String
    ): AnimeEpisodeTable {
        return if (kitsuEpisode != null) {
            val imageEpisode = if(kitsuEpisode.attributes?.thumbnail != null) {
                if(kitsuEpisode.attributes.thumbnail.large != null) {
                    imageService.saveFileInSThird(
                        "images/episodes/$urlLinking/$episode.png",
                        URL(kitsuEpisode.attributes.thumbnail.large).readBytes(),
                        compress = false,
                    )
                } else {
                    if(kitsuEpisode.attributes.thumbnail.original != null) {
                        imageService.saveFileInSThird(
                            "images/episodes/$urlLinking/$episode.png",
                            URL(kitsuEpisode.attributes.thumbnail.original).readBytes(),
                            compress = true,
                            width = 400,
                            height = 225
                        )
                    } else imageDefault
                }
            } else ""

            return AnimeEpisodeTable(
                link = "$playerLink?episode=$episode",
                title = titleRu,
                titleEn = kitsuEpisode.attributes?.titles?.enToUs ?: "",
                description = descriptionRu,
                descriptionEn = kitsuEpisode.attributes?.description ?: "",
                number = kitsuEpisode.attributes?.number ?: episode,
                image = imageEpisode
            )

        } else {
            AnimeEpisodeTable(
                link = "$playerLink?episode=$episode",
                title = episode.toString(),
                titleEn = episode.toString(),
                description = null,
                descriptionEn = null,
                number = episode,
                image = null
            )
        }
    }

    fun findEpisodeByNumber(number: Int, kitsuEpisodes: List<EpisodesKitsu>): EpisodesKitsu? {
        return kitsuEpisodes.find { kitsuEpisode ->
            kitsuEpisode.attributes?.number == number
        }
    }

    suspend fun translateText(text: List<TextMicRequest>): List<String> {
        return if (text.isNotEmpty()) {
            val translatedText = try {
                client.post {
                    bearerAuth(client.get {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = "edge.microsoft.com"
                            encodedPath = "/translate/auth"
                        }
                        header("Accept", "application/vnd.api+json")
                    }.bodyAsText())
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "api-edge.cognitive.microsofttranslator.com"
                        encodedPath = "/translate"
                    }
                    setBody(text)
                    header("Accept", "application/vnd.api+json")
                    parameter("from", "en")
                    parameter("to", "ru")
                    parameter("api-version", "3.0")
                }.body<List<TextTranslations>>()
            } catch (e: Exception) {
                delay(1000)
                client.post {
                    bearerAuth(client.get {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = "edge.microsoft.com"
                            encodedPath = "/translate/auth"
                        }
                        header("Accept", "application/vnd.api+json")
                    }.bodyAsText())
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "api-edge.cognitive.microsofttranslator.com"
                        encodedPath = "/translate"
                    }
                    setBody(text)
                    header("Accept", "application/vnd.api+json")
                    parameter("from", "en")
                    parameter("to", "ru")
                    parameter("api-version", "3.0")
                }.body<List<TextTranslations>>()
            }
            val tempResult = mutableListOf<String>()

            translatedText.forEach {
                it.translations.forEach {
                    tempResult.add(it.text ?: "")
                }
            }
            return tempResult
        } else listOf()
    }

    fun mergeNumbers(numbers: List<Int>): String {
        if(numbers.isNotEmpty()) {
            val ranges = mutableListOf<Pair<Int, Int>>()
            var currentRange = Pair(numbers.first(), numbers.first())

            for (i in 1 until numbers.size) {
                currentRange = if (numbers[i] == currentRange.second + 1) {
                    Pair(currentRange.first, numbers[i])
                } else {
                    ranges.add(currentRange)
                    Pair(numbers[i], numbers[i])
                }
            }

            ranges.add(currentRange)

            return ranges.joinToString(", ") { if (it.first == it.second) it.first.toString() else "${it.first}-${it.second}" }
        } else return ""
    }

    fun checkAnime(url: String): AnimeTable {
        return animeRepository.findByUrl(url).orElseThrow { NotFoundException("Anime not found") }
    }

    fun checkKodikTranslation(translationId: Int, title: String, voice: String): AnimeTranslationTable {
        val translationCheck = animeTranslationRepository.findById(translationId).isPresent
        return if(translationCheck) {
            animeTranslationRepository.findById(translationId).get()
        } else {
            if(translationId == 1002) {
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
            } else animeTranslationRepository.save(
                AnimeTranslationTable(
                    id = translationId,
                    title = title,
                    voice = voice
                )
            )
        }
    }
}
