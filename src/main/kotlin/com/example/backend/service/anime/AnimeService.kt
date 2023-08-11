package com.example.backend.service.anime


import com.example.backend.jpa.anime.*
import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.models.ServiceResponse
import com.example.backend.models.anime.*
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
import com.example.backend.models.animeResponse.episode.EpisodeLight
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.light.AnimeLightWithType
import com.example.backend.models.animeResponse.media.AnimeMediaResponse
import com.example.backend.models.jikan.*
import com.example.backend.models.users.StatusFavourite
import com.example.backend.repository.anime.*
import com.example.backend.repository.anime.error.AnimeErrorParserRepository
import com.example.backend.repository.user.anime.UserRatingCountRepository
import com.example.backend.service.image.ImageService
import com.example.backend.util.*
import com.example.backend.util.common.*
import com.example.backend.util.exceptions.NotFoundException
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import org.hibernate.CacheMode
import org.hibernate.search.jpa.Search
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
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
import org.springframework.transaction.annotation.Transactional
import javax.validation.constraints.Max
import javax.validation.constraints.Min


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
    lateinit var animeTranslationCountRepository: AnimeTranslationCountRepository

    @Autowired
    lateinit var animeEpisodeTranslationRepository: AnimeEpisodeTranslationRepository

    @Autowired
    lateinit var animeMediaRepository: AnimeMediaRepository

    @Autowired
    lateinit var animeRepository: AnimeRepository

    @Autowired
    lateinit var animeBlockedRepository: AnimeBlockedRepository

    @Autowired
    lateinit var animeMusicRepository: AnimeMusicRepository

    @Autowired
    private lateinit var userRatingCountRepository: UserRatingCountRepository

    @Autowired
    private lateinit var animeErrorParserRepository: AnimeErrorParserRepository

    @Autowired
    private lateinit var animeRelatedRepository: AnimeRelatedRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager



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
//        install(Logging) {
//            logger = Logger.DEFAULT
//            level = LogLevel.ALL
//        }
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

        val pageable: Pageable = PageRequest.of(pageNum, pageSize)

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
                    studio = studio,
                    order = order
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
        translationIds: List<String>?,
        order: String?
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
            val translationJoin = root.join<AnimeTable, AnimeTranslationTable>("translations")

            val translationIdsPredicate = criteriaBuilder.isTrue(
                translationJoin.get<AnimeTranslationTable>("id").`in`(
                    translationIds.mapNotNull { it.toIntOrNull() }.toList()
                )
            )

            predicates.add(translationIdsPredicate)
        }
        if (predicates.isNotEmpty()) {
            if (searchQuery == null) {
                criteriaQuery.distinct(true).where(criteriaBuilder.and(*predicates.toTypedArray()))
            } else
                criteriaQuery.distinct(true).where(criteriaBuilder.or(*predicates.toTypedArray()))
        }

        val sort: List<Order> = when (order) {
            "popular" -> listOf(
                criteriaBuilder.desc(root.get<AnimeTable>("views")),
                criteriaBuilder.desc(root.get<AnimeTable>("countRate"))
            )
            "dateASC" -> listOf(criteriaBuilder.asc(root.get<AnimeTable>("airedAt")))
            "dateDESC" -> listOf(criteriaBuilder.desc(root.get<AnimeTable>("airedAt")))
            "shikimoriRating" -> listOf(criteriaBuilder.desc(root.get<AnimeTable>("shikimoriRating")))
            "random" -> listOf(criteriaBuilder.asc(criteriaBuilder.function("random", AnimeTable::class.java)))
            "views" -> listOf(criteriaBuilder.desc(root.get<AnimeTable>("views")))
            else -> emptyList()
        }

        criteriaQuery.orderBy(sort)

        val query = entityManager.createQuery(criteriaQuery)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return query.resultList
    }

    fun levenshteinDistance(s1: String, s2: String): Int {

        val distances = Array(s1.length + 1) { MutableList(s2.length + 1) {0} }

        for (i in 0..s1.length) distances[i][0] = i
        for (j in 0..s2.length) distances[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i-1] == s2[j-1]) {
                    distances[i][j] = distances[i - 1][j - 1]
                } else {
                    distances[i][j] = minOf(
                        distances[i-1][j] + 1,
                        distances[i][j-1] + 1,
                        distances[i-1][j-1] + 1
                    )
                }
            }
        }

        return distances[s1.length][s2.length]
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

    override fun getAnimeByUrl(url: String): AnimeDetail {
        return animeTableToAnimeDetail(checkAnime(url))
    }

    override fun getAnimeRelated(url: String): ServiceResponse<AnimeLightWithType> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("related", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime with url = $url not found")
        } else {
            val relatedAnimeList: List<AnimeLightWithType> = anime[0].related.mapNotNull { related ->
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
        }
    }


    override fun getAnimeSimilar(url: String): ServiceResponse<AnimeLight> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("similarAnime", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime = entityManager.createQuery(criteriaQuery).resultList

        if (anime.isEmpty()) {
            throw NotFoundException("Anime not found")
        } else {
            val similarCriteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
            val similarRoot = similarCriteriaQuery.from(AnimeTable::class.java)
            similarCriteriaQuery.select(similarRoot)
                .where(similarRoot.get<Int>("shikimoriId").`in`(anime[0].similarAnime))

            val similarEntityList = entityManager.createQuery(similarCriteriaQuery).resultList as List<AnimeTable>

            val similarEntityMap: Map<Int, AnimeTable> = similarEntityList.associateBy { it.shikimoriId }

            val similarAnimeList: List<AnimeTable> = anime[0].similarAnime.mapNotNull { similarAnimeId ->
                similarEntityMap[similarAnimeId]
            }

            return if (similarAnimeList.isNotEmpty()) {
                animeLightSuccess(listToAnimeLight(similarAnimeList))
            } else ServiceResponse(
                data = null,
                message = "Success",
                status = HttpStatus.OK
            )
        }
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

    override fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount> {
        val anime: AnimeTable = checkAnime(url)

        val translationsCountEpisodes: Set<AnimeEpisodeTranslationCount> = anime.translationsCountEpisodes

        return translationsCountEpisodes.map { translationEpisodes ->
            AnimeTranslationCount(
                translation = translationEpisodes.translation,
                countEpisodes = translationEpisodes.countEpisodes
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

    override fun getAnimeEpisodesWithPaging(url: String, pageNumber: Int, pageSize: Int, sort: String?): List<EpisodeLight> {
        val anime: AnimeTable = checkAnime(url)

        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeTable> = criteriaBuilder.createQuery(AnimeEpisodeTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val episodesJoin = animeRoot.join<AnimeTable, AnimeEpisodeTable>("episodes", JoinType.LEFT)

        criteriaQuery.where(criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url))

        criteriaQuery.select(episodesJoin)

        when(sort) {
            "numberAsc" -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
            "numberDesc" -> criteriaQuery.orderBy(criteriaBuilder.desc(episodesJoin.get<Int>("number")))
            else -> criteriaQuery.orderBy(criteriaBuilder.asc(episodesJoin.get<Int>("number")))
        }

        val query = entityManager.createQuery(criteriaQuery)

        val firstResult = (pageNumber - 1) * pageSize
        query.firstResult = if (firstResult >= 0) firstResult else 0
        query.maxResults = pageSize

        return episodeToEpisodeLight(query.resultList)
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

    fun setBlockedAnime() {
        val temp = mutableListOf<AnimeBlockedTable>()
        val blockedIds = listOf(
            6864, 4918, 52198, 37517, 1535, 34542, 22319, 7088, 10465, 8577,
            40010, 6987, 30831, 38040, 38924, 6201, 17729, 19429, 24833, 35241,
            37998, 34177, 34019, 39469, 36632, 32949, 40314, 34048, 16363, 11859,
            8456, 30679, 35849, 31491, 18039, 3889, 15391, 12581, 14893, 25, 40010,
            6791, 9624, 9515, 6392, 36560, 23423, 21679, 10582, 11209, 9744, 35000,
            9201
        )

        blockedIds.forEach { id ->
            temp.add(
                AnimeBlockedTable(
                    shikimoriID = id,
                    type = AnimeBlockedType.ALL
                )
            )
        }

        animeBlockedRepository.saveAll(temp)
    }

    @Transactional
    fun checkBlockedAnime() {
        animeBlockedRepository.findAll().forEach { blocked ->
            animeRepository.findByShikimoriId(blocked.shikimoriID).let { anime ->
                anime.get().apply {
                    related.clear()
                    episodes.clear()
                    translationsCountEpisodes.clear()
                    favorites.clear()
                    rating.clear()
                    music.clear()
                    translations.clear()
                    genres.clear()
                    media.clear()
                    studios.clear()
                    titleEn.clear()
                    titleJapan.clear()
                    synonyms.clear()
                    otherTitles.clear()
                    similarAnime.clear()
                    screenshots.clear()
                    ids = AnimeIds()
                    images = AnimeImages()
                }
                animeRepository.delete(anime.get())
            }
        }
    }

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
                parameter("types", "anime-serial, anime")
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
            ar.result.distinctBy { it.shikimoriId }.forEach Loop@ { animeTemp ->
                try {
                    val anime = checkKodikSingle(animeTemp.shikimoriId, translationID)

                    val shikimori = checkShikimori(animeTemp.shikimoriId)

                    var userRatesStats = 0

                    shikimori?.usersRatesStats?.forEach {
                        userRatesStats += it.value
                    }

                    if (
                        !anime.materialData.title.contains("Атака Титанов") &&
                        !anime.materialData.title.contains("Атака титанов") && !animeBlockedRepository.findById(animeTemp.shikimoriId.toInt()).isPresent && anime.materialData.shikimoriVotes > 90 && userRatesStats > 1000 && shikimori != null
                    ) {
                        println(anime.shikimoriId.toInt())
                        val tempingAnime = animeRepository.findByShikimoriId(anime.shikimoriId.toInt())

                        if (!tempingAnime.isPresent) {
                            val startTime = System.currentTimeMillis()

                            val g = mutableListOf<AnimeGenreTable>()
                            anime.materialData.genres.forEach { genre ->
                                if (genre in listOf("яой", "эротика", "хентай", "Яой", "Хентай", "Эротика")) {
                                    return@forEach
                                }

                                val existingGenre = animeGenreRepository.findByGenre(genre).orElse(null)
                                if (existingGenre != null) {
                                    g.add(existingGenre)
                                } else {
                                    val newGenre = AnimeGenreTable(genre = genre)
                                    animeGenreRepository.save(newGenre)
                                    g.add(newGenre)
                                }
                            }

                            val st = mutableListOf<AnimeStudiosTable>()
                            anime.materialData.animeStudios.forEach { studio ->
                                val existingStudio = animeStudiosRepository.findByStudio(studio).orElse(null)
                                if (existingStudio != null) {
                                    st.add(existingStudio)
                                } else {
                                    val newStudio = AnimeStudiosTable(studio = studio)
                                    animeStudiosRepository.save(newStudio)
                                    st.add(newStudio)
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

                            val f = shikimori.russianLic
                            val zx = shikimori.russian

                            var urlLinking = translit(if (f != null && checkEnglishLetter(zx)) f else zx)

                            if(animeRepository.findByUrl(urlLinking).isPresent) {
                                urlLinking = "${translit(if (f != null && checkEnglishLetter(zx)) f else zx)}-${if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.year}"
                            }

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
                                val temp = relationIdsDeferred.await()
                                if(temp != null) {
                                    relationIds.addAll(temp)
                                }
                            }

                            val r = mutableListOf<AnimeRelatedTable>()

                            val relationsToSave = relationIds.take(30).map { it ->
                                val shikimoriId = if (it.anime != null) it.anime.id else it.manga!!.id
                                AnimeRelatedTable(
                                    type = it.relationRussian.toString(),
                                    shikimoriId = shikimoriId,
                                    typeEn = it.relation.toString()
                                )
                            }
                            r.addAll(animeRelatedRepository.saveAll(relationsToSave))

                            val similarIdsFlow = flow {
                                delay(1000)
                                val similarIds = client.get {
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

                                emit(similarIds)
                            }.flowOn(Dispatchers.IO)

                            val media = shikimori.videos.map { video ->
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
                            var imagesCallback: ((AnimeImagesTypes?, AnimeBufferedImagesSup?) -> Unit)? = null

                            // Функция инициализации
                            fun initImages(callback: (AnimeImagesTypes?, AnimeBufferedImagesSup?) -> Unit) {

                                // Передаем callback
                                imagesCallback = callback

                                // Запускаем корутину
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

                                        // Вызываем callback с результатами
                                    imagesCallback?.invoke(animeImages, image)

                                }

                            }
                            var aI: AnimeImagesTypes? = null
                            var aB: AnimeBufferedImagesSup? = null
                            initImages { animeImages, image ->
                                aI = animeImages
                                aB = image
                            }

                            val episodesReady = mutableListOf<AnimeEpisodeTable>()

                            when(anime.type) {
                                "anime-serial" -> {
                                    anime.seasons.forEach { kodikSeason ->
                                        if (kodikSeason.key != "0") {
                                            val jikanEpisodes = mutableListOf<JikanEpisode>()
                                            val kitsuEpisodes = mutableListOf<EpisodesKitsu>()

                                            // Run kitsu.io and jikan.moe API calls in parallel using async and await
                                            runBlocking {
                                                val deferredKitsu = async {
                                                    val kitsuAsyncTask = async { fetchKitsuEpisodes("api/edge/anime/${animeIds.kitsu}/episodes") }

                                                    var responseKitsuEpisodes = kitsuAsyncTask.await()
                                                    while (responseKitsuEpisodes.data != null) {
                                                        kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
                                                        val kitsuUrl = responseKitsuEpisodes.links.next?.replace("https://kitsu.io", "").toString()
                                                        responseKitsuEpisodes = if(kitsuUrl != "null") fetchKitsuEpisodes(kitsuUrl) else KitsuDefaults()
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
                                                        anime.link,
                                                        urlLinking,
                                                        kodikSeason.value.episodes,
                                                        kitsuEpisodes,
                                                        jikanEpisodes,
                                                        animeImages!!.medium
                                                    )
                                                }
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
                                            null
                                        )
                                        episodesReady.addAll(addEpisodeTranslations(listOf(episode), anime.shikimoriId, "anime"))
                                    }
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
                                val musicToSave = mutableListOf<AnimeMusicTable>()

                                if (jikanData != null) {
                                    jikanData.endings.forEach { ending ->
                                        if (ending != null) {
                                            val endingNormalize = jikanThemesNormalize(ending)
                                            musicToSave.add(
                                                AnimeMusicTable(
                                                    url = "https://music.youtube.com/search?q=$endingNormalize",
                                                    name = endingNormalize,
                                                    episodes = mergeEpisodes(ending),
                                                    type = AnimeMusicType.Ending,
                                                    hosting = "YoutubeMusic"
                                                )
                                            )
                                        }
                                    }

                                    jikanData.openings.forEach { opening ->
                                        if (opening != null) {
                                            val openingNormalize = jikanThemesNormalize(opening)
                                            musicToSave.add(
                                                AnimeMusicTable(
                                                    url = "https://music.youtube.com/search?q=$openingNormalize",
                                                    name = openingNormalize,
                                                    episodes = mergeEpisodes(opening),
                                                    type = AnimeMusicType.Opening,
                                                    hosting = "YoutubeMusic"
                                                )
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
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}/screenshots"
                                    }
                                }.body<List<ScreenshotsParse>>().map { screenshot ->
                                    "https://shikimori.me${screenshot.original}"
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

                            otherTitles.add(if (f != null && checkEnglishLetter(zx)) f else zx)

                            val translations = episodesReady
                                .flatMap { episode ->
                                    episode.translations
                                        .map {
                                            it.translation
                                        }
                                }
                                .distinct()
                                .toMutableList()

                            val translationCounts = episodesReady
                                .flatMap { episode -> episode.translations }
                                .groupBy { translation -> translation.translation.id }
                                .mapValues { (_, translations) -> translations.size }

                            val translationsCountReady = translationCounts.map { (translationId, count) ->
                                AnimeEpisodeTranslationCount(
                                    translation = animeTranslationRepository.findById(translationId).get(),
                                    countEpisodes = count
                                )
                            }.let { animeTranslationCountRepository.saveAll(it) }
                                .toList()

                            val a = AnimeTable(
                                title = if (f != null && checkEnglishLetter(zx)) f else zx,
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
                                nextEpisode = if (shikimori.nextEpisodeAt != null) {
                                    LocalDateTime.parse(shikimori.nextEpisodeAt, formatterUpdated)
                                } else {
                                    null
                                },
                                images = AnimeImages(
                                    large = animeImages?.large ?: "",
                                    medium = animeImages?.medium ?: "",
                                    cover = animeImages?.cover ?: ""
                                ),
                                titleEn = shikimori.english.toMutableList(),
                                titleJapan = shikimori.japanese.toMutableList(),
                                synonyms = shikimori.synonyms.toMutableList(),
                                otherTitles = otherTitles,
                                similarAnime = similarIds.take(30).toMutableList(),
                                status = shikimori.status,
                                description = shikimori.description.replace(Regex("\\[\\/?[a-z]+.*?\\]"), ""),
                                year = if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt).year else anime.materialData.year,
                                createdAt = anime.createdAt,
                                link = anime.link,
                                airedAt = if (shikimori.airedAt != null) LocalDate.parse(shikimori.airedAt) else anime.materialData.airedAt,
                                releasedAt = if (shikimori.releasedAt != null) LocalDate.parse(shikimori.releasedAt) else anime.materialData.releasedAt,
                                episodesCount = shikimori.episodes,
                                episodesAires = if (shikimori.status == "released") shikimori.episodes else episodesReady.size,
                                type = anime.materialData.animeType,
                                updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime(),
                                minimalAge = when (shikimori.rating) {
                                    "g" -> 0
                                    "pg" -> 12
                                    "pg_13" -> 16
                                    "r" -> 18
                                    "r_plus" -> 18
                                    else -> 0
                                },
                                screenshots = screenShots,
                                ratingMpa = when (shikimori.rating) {
                                    "g" -> "G"
                                    "pg" -> "PG"
                                    "pg_13" -> "PG-13"
                                    "r" -> "R"
                                    "r_plus" -> "R+"
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
                            a.addTranslationCount(translationsCountReady)
                            a.addRelated(r)
                            a.addTranslation(translations)
                            a.addEpisodesAll(episodesReady)
                            a.addAllMusic(music)
                            a.addAllAnimeGenre(g)
                            a.addAllAnimeStudios(st)
                            a.addMediaAll(media.filterNotNull())

                            val preparationToSaveAnime = animeRepository.findByShikimoriId(a.shikimoriId)
                            if(preparationToSaveAnime.isPresent) {
                                return@Loop
                            } else animeRepository.saveAndFlush(a)

                            val endTime = System.currentTimeMillis()
                            val executionTime = endTime - startTime
                            println("Время выполнения запроса: ${executionTime} мс")
                        }
                    }
                } catch (e: Exception) {
                    e.stackTrace.forEach {
                        println(it)
                    }
                    animeErrorParserRepository.save(
                        AnimeErrorParserTable(
                            message = e.message,
                            cause = "",
                            shikimoriId = animeTemp.shikimoriId.toInt()
                        )
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


    override fun updateEpisodes(translationID: String){
        val animeBaseList = animeRepository.findByIdForEpisodesUpdate("ongoing")

        animeBaseList.forEach { animeBase ->
            val anime = checkKodikSingle(animeBase.shikimoriId.toString(), translationID)

            val shikimori = checkShikimori(animeBase.shikimoriId.toString())

            if(animeBase.nextEpisode != null || !animeBase.updatedAt.isBefore(LocalDateTime.now().minusWeeks(1))) {
                val animeIds: AnimeIds = animeBase.ids
                val animeImages: AnimeImages = animeBase.images
                val episodesReady = mutableListOf<AnimeEpisodeTable>()

                when(anime.type) {
                    "anime-serial" -> {
                        anime.seasons.forEach { kodikSeason ->
                            if (kodikSeason.key != "0") {
                                val jikanEpisodes = mutableListOf<JikanEpisode>()
                                val kitsuEpisodes = mutableListOf<EpisodesKitsu>()

                                // Run kitsu.io and jikan.moe API calls in parallel using async and await
                                runBlocking {
                                    val deferredKitsu = async {
                                        val kitsuAsyncTask =
                                            async { fetchKitsuEpisodes("api/edge/anime/${animeIds.kitsu}/episodes") }

                                        var responseKitsuEpisodes = kitsuAsyncTask.await()
                                        while (responseKitsuEpisodes.data != null) {
                                            kitsuEpisodes.addAll(responseKitsuEpisodes.data!!)
                                            val kitsuUrl =
                                                responseKitsuEpisodes.links.next?.replace("https://kitsu.io", "")
                                                    .toString()
                                            responseKitsuEpisodes =
                                                if (kitsuUrl != "null") fetchKitsuEpisodes(kitsuUrl) else KitsuDefaults()
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
                                            anime.link,
                                            animeBase.url,
                                            kodikSeason.value.episodes,
                                            kitsuEpisodes,
                                            jikanEpisodes,
                                            animeImages.medium
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                val translations = episodesReady
                    .flatMap { episode ->
                        episode.translations
                            .map {
                                it.translation
                            }
                    }
                    .distinct()
                    .toMutableList()

                val translationCounts = episodesReady
                    .flatMap { episode -> episode.translations }
                    .groupBy { translation -> translation.translation.id }
                    .mapValues { (_, translations) -> translations.size }

                val translationsCountReady = translationCounts.map { (translationId, count) ->
                    AnimeEpisodeTranslationCount(
                        translation = animeTranslationRepository.findById(translationId).get(),
                        countEpisodes = count
                    )
                }.toList()
                val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.of("Europe/Moscow"))

                animeBase.episodesAires = episodesReady.size
                animeBase.updatedAt = LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
                if(episodesReady.size == animeBase.episodesCount && !animeBase.updatedAt.isBefore(LocalDateTime.now().minusWeeks(1)))
                    animeBase.status = "released"
                if (shikimori != null) {
                    animeBase.nextEpisode = if (shikimori.nextEpisodeAt != null) {
                        LocalDateTime.parse(shikimori.nextEpisodeAt, formatterUpdated)
                    } else {
                        null
                    }
                }
                animeBase.addTranslationCount(translationsCountReady)
                animeBase.addTranslation(translations)
                animeBase.addEpisodesAll(episodesReady)
                animeRepository.save(animeBase)
            }
        }
    }

    suspend fun fetchKitsuEpisodes(url: String): KitsuDefaults<EpisodesKitsu> {
        delay(1000)
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "kitsu.io"
                encodedPath = url
            }
            header("Accept", "application/vnd.api+json")
        }.body<KitsuDefaults<EpisodesKitsu>>()
    }

    suspend fun fetchJikanEpisodes(page: Int, shikimoriId: String): JikanDefaults<JikanEpisode> {
        delay(1000)
        return client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.jikan.moe"
                encodedPath = "/v4/anime/${shikimoriId}/episodes"
            }
            parameter("page", page)
        }.body<JikanDefaults<JikanEpisode>>()
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
    suspend fun processEpisodes(
        type: String,
        shikimoriId: String,
        playerLink: String,
        urlLinking: String,
        kodikEpisodes: Map<String, Episode>,
        kitsuEpisodes: List<EpisodesKitsu>,
        jikanEpisodes: List<JikanEpisode>,
        imageDefault: String
    ): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()

        val kitsuEpisodesMapped = mutableMapOf<String, EpisodesKitsu?>()
        val translatedTitleMapped = mutableMapOf<String, String>()
        val translatedDescriptionMapped = mutableMapOf<String, String>()
        val tempTranslatedTitle = mutableListOf<TextMicRequest>()
        val tempTranslatedDescription = mutableListOf<TextMicRequest>()

        if(jikanEpisodes.size >= kodikEpisodes.size) {
            jikanEpisodes.map { episode ->
                val number = episode.id
                val kitsuEpisode = findEpisodeByNumber(number, kitsuEpisodes)
                kitsuEpisodesMapped[number.toString()] = kitsuEpisode
                translatedTitleMapped[number.toString()] = jikanEpisodes[number-1].title
                translatedDescriptionMapped[number.toString()] = kitsuEpisode?.attributes?.description ?: ""
            }
        } else {
            kodikEpisodes.map { (episodeKey, episode) ->
                if(episodeKey.toInt() <= kitsuEpisodes.size) {
                    val kitsuEpisode = findEpisodeByNumber(episodeKey.toInt(), kitsuEpisodes)
                    kitsuEpisodesMapped[episodeKey] = kitsuEpisode
                    translatedDescriptionMapped[episodeKey] = kitsuEpisode?.attributes?.description ?: ""
                }
                if (episodeKey.toInt() <= jikanEpisodes.size) {
                    translatedTitleMapped[episodeKey] = when (episodeKey) {
                        "0" -> {
                            if (kodikEpisodes["0"] != null && jikanEpisodes[episodeKey.toInt()].id != 0)
                                episodeKey
                            else jikanEpisodes[episodeKey.toInt()].title
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
            val episodeKeyList = when(episodeKey) {
                "0" -> {
                    episodeKey.toInt()
                }
                "1" -> {
                    if(translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
                }
                else -> {
                    if(translatedTitleMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
                }
            }
            translatedTitleMapped[episodeKey] = a[episodeKeyList]
        }

        translatedDescriptionMapped.map { (episodeKey, title) ->
            val number = if(translatedDescriptionMapped["0"] != null) episodeKey.toInt() else episodeKey.toInt() - 1
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
                    }
                )
            }
        }

        val processedEpisodes = jobs.awaitAll()
        val sortedEpisodes = processedEpisodes.sortedBy { it.number }

        episodeReady.addAll(addEpisodeTranslations(sortedEpisodes, shikimoriId, "anime-serial"))

        return episodeReady
    }

    fun addEpisodeTranslations(episodes: List<AnimeEpisodeTable>, shikimoriId: String, type: String): List<AnimeEpisodeTable> {
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
                parameter("types", type)
                parameter("camrip", false)
                parameter("with_episodes_data", true)
                parameter("not_blocked_in", "ALL")
                parameter("with_material_data", true)
                parameter("shikimori_id", shikimoriId)
                parameter(
                    "anime_genres",
                    "безумие, боевые искусства, вампиры, военное, гарем, демоны, детектив, детское, дзёсей, драма, игры, исторический, комедия, космос, машины, меха, музыка, пародия, повседневность, полиция, приключения, психологическое, романтика, самураи, сверхъестественное, спорт, супер сила, сэйнэн, сёдзё, сёдзё-ай, сёнен, сёнен-ай, триллер, ужасы, фантастика, фэнтези, школа, экшен"
                )
                parameter("translation_id", "610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002, 1978, 1291, 1272, 1946")
            }.body<AnimeResponse<AnimeParser>>()
        }

        val episodeTranslationsToSave = mutableListOf<EpisodeTranslation>()

        animeVariations.result.forEach { anime ->
            episodes.forEach { episode ->
                if (episode.number <= anime.lastEpisode || anime.lastEpisode == 0) {
                    val translationId = when(anime.translation.id) {
                        1002 -> {
                            643
                        }
                        else -> anime.translation.id
                    }
                    val episodeTranslation = EpisodeTranslation(
                        translation = animeTranslationRepository.findById(translationId).get(),
                        link = if(type == "anime-serial") "${anime.link}?episode=${episode.number}" else anime.link
                    )
                    episode.addTranslation(episodeTranslation)
                    episodeTranslationsToSave.add(episodeTranslation)
                }
            }
        }

        animeEpisodeTranslationRepository.saveAll(episodeTranslationsToSave)

        return episodes
    }

    suspend fun processEpisode(
        type: String,
        shikimoriId: String,
        urlLinking: String,
        episode: Int,
        kitsuEpisode: EpisodesKitsu?,
        titleRu: String?,
        descriptionRu: String?,
        imageDefault: String,
        jikanEpisode: JikanEpisode?
    ): AnimeEpisodeTable {
        println("EPISODE EPISODE EPISODE")
        return if (kitsuEpisode != null) {
            val imageEpisode = try {
                if(kitsuEpisode.attributes?.thumbnail != null) {
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
            } catch (e: Exception) {
                ""
            }


            val kitsuNumber = when {
                kitsuEpisode.attributes?.number != null && kitsuEpisode.attributes.number == episode -> kitsuEpisode.attributes.number
                episode == 0 -> 0
                else -> episode
            }

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
                title = if(titleRu != null && titleRu.length > 3) titleRu else "$episode",
                titleEn = kitsuEpisode.attributes?.titles?.enToUs ?: "",
                description = descriptionRu,
                descriptionEn = kitsuEpisode.attributes?.description ?: "",
                number = kitsuNumber,
                image = if(imageEpisode.length > 5) imageEpisode else imageDefault,
                filler = jikanEpisode?.filler ?: false,
                recap = jikanEpisode?.recap ?: false,
                aired = if(airedDate != null) LocalDate.parse(if(airedDate.length > 10) airedDate.substring(0, 10) else airedDate, DateTimeFormatter.ISO_DATE) else null
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
                aired = if(airedDate != null) LocalDate.parse(if(airedDate.length > 10) airedDate.substring(0, 10) else airedDate, DateTimeFormatter.ISO_DATE) else null
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
                try {
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
                } catch (e: Exception) {
                    return listOf()
                }
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

    fun checkShikimori(shikimoriId: String): AnimeMediaParse? {
        return try {
            runBlocking {
                client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "shikimori.me/api/animes/${shikimoriId}"
                    }
                }.body<AnimeMediaParse>()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun checkKodikSingle(shikimoriId: String, translationID: String): AnimeParser {
        return runBlocking {
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
                parameter("sort", "shikimori_rating")
                parameter("order", "desc")
                parameter("types", "anime-serial, anime")
                parameter("camrip", false)
                parameter("shikimori_id", shikimoriId)
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
    }

}


