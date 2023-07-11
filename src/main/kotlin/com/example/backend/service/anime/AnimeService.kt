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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
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

    override fun getAnimeEpisodesWithPaging(url: String, pageNumber: Int, pageSize: Int, sort: String?): List<EpisodeLight> {
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

            return episodeToEpisodeLight(episodes)
        }

        return emptyList()
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
            }.body<AnimeResponse>()
        }

        val translatedToken = runBlocking {
            client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "edge.microsoft.com"
                    encodedPath = "/translate/auth"
                }
                header("Accept", "application/vnd.api+json")
            }.bodyAsText()
        }

        try {
        while (nextPage != null) {
            ar.result.forEach Loop@{ animeTemp ->
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
                    }.body<AnimeResponse>()
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
                    try {
                        val tempingAnime = animeRepository.findByShikimoriId(anime.shikimoriId.toInt())

                        if (!tempingAnime.isPresent) {
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

                            Thread.sleep(1000)
                            val mediaTemp = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}"
                                    }
                                }.body<AnimeMediaParse>()
                            }

                            val urlLinking = translit(mediaTemp.russian)

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
                                        processEpisodes(urlLinking, kodikSeason.value.episodes, kitsuEpisodes)
                                    })
                                }
                            }
                            Thread.sleep(1000)
                            val relationIds = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}/related"
                                    }
                                }.body<List<RelationParse>>()
                            }

                            val r = mutableListOf<AnimeRelatedTable>()

                            try {
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
                            } catch (_: Exception) {
                            }

                            Thread.sleep(1000)
                            val similarIds = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}/similar"
                                    }
                                }.body<List<SimilarParse>>()
                            }.flatMap { similar ->
                                listOfNotNull(similar.id)
                            }.map { it }

                            val translations: MutableList<AnimeTranslationTable> = mutableListOf()

                            mediaTemp.fandubbers.forEach Translations@{
                                val checkTrans = checkTranslation(it)
                                if (checkTrans.id == 0) return@Translations
                                val translationIs = animeTranslationRepository.findById(checkTrans.id).isPresent
                                translations.add(
                                    if (translationIs) {
                                        animeTranslationRepository.findById(checkTrans.id).get()
                                    } else {
                                        animeTranslationRepository.save(checkTrans)
                                    }
                                )
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

                            var kitsuAnime = runBlocking {
                                client.get {
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "kitsu.io"
                                        encodedPath = "/api/edge/anime/${animeIds.kitsu}"
                                    }
                                    header("Accept", "application/vnd.api+json")
                                }.body<KitsuDetails<AnimeKitsu>>()
                            }

                            Thread.sleep(1000)
                            val jikanImage = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "${Constants.Jikan}${Constants.jikanAnime}${anime.shikimoriId}"
                                    }
                                }.body<Jikan<JikanData>>()
                            }

                            if(kitsuAnime.data != null) {
                                animeImages = AnimeImagesTypes(
                                    large = imageService.saveFileInSThird(
                                        "images/large/$urlLinking.png",
                                        URL(kitsuAnime.data!!.attributesKitsu.posterImage.original).readBytes(),
                                        compress = true,
                                        width = 400,
                                        height = 640
                                    ),
                                    medium = imageService.saveFileInSThird(
                                        "images/medium/$urlLinking.png",
                                        URL(kitsuAnime.data!!.attributesKitsu.posterImage.original).readBytes(),
                                        compress = true,
                                        width = 200,
                                        height = 440
                                    ),
                                    cover = imageService.saveFileInSThird(
                                        "images/cover/$urlLinking.png",
                                        URL(kitsuAnime.data!!.attributesKitsu.coverImage.coverLarge).readBytes(),
                                        compress = false,
                                    ),
                                )
                                image = AnimeBufferedImagesSup(
                                    large = ImageIO.read(URL(kitsuAnime.data!!.attributesKitsu.posterImage.original)),
                                    medium = ImageIO.read(URL(kitsuAnime.data!!.attributesKitsu.posterImage.large)),
                                )
                            } else if (jikanImage.data != null) {
                                animeImages = AnimeImagesTypes(
                                    large = imageService.saveFileInSThird(
                                        "images/large/$urlLinking.png",
                                        URL(jikanImage.data.images.jikanJpg.largeImageUrl).readBytes()
                                    ),
                                    medium = imageService.saveFileInSThird(
                                        "images/medium/$urlLinking.png",
                                        URL(jikanImage.data.images.jikanJpg.mediumImageUrl).readBytes()
                                    )
                                )
                                image = AnimeBufferedImagesSup(
                                    large = ImageIO.read(URL(jikanImage.data.images.jikanJpg.largeImageUrl)),
                                    medium = ImageIO.read(URL(jikanImage.data.images.jikanJpg.mediumImageUrl)),
                                )
                            }

                            Thread.sleep(1000)
                            val jikanThemes = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "${Constants.Jikan}${Constants.jikanAnime}${anime.shikimoriId}/themes"
                                    }
                                }.body<Jikan<JikanThemes>>()
                            }

                            val music: MutableList<AnimeMusicTable> = mutableListOf()

                            if (jikanThemes.data != null) {
                                jikanThemes.data.endings.forEach { ending ->
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
                                jikanThemes.data.openings.forEach { opening ->
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

                            Thread.sleep(1000)
                            val screenshots = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}/screenshots"
                                    }
                                }.body<List<ScreenshotsParse>>()
                            }.map { screenshot ->
                                "https://shikimori.me${screenshot.original}"
                            }

                            val formatterUpdated = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                .withZone(ZoneId.of("Europe/Moscow"))

                            val otherTitles = anime.materialData.otherTitles.toMutableList()

                            otherTitles.add(mediaTemp.russianLic ?: mediaTemp.russian)

                            val a = AnimeTable(
                                title = mediaTemp.russianLic ?: mediaTemp.russian,
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
                                    large = animeImages?.large!!,
                                    medium = animeImages.medium,
                                    cover = animeImages.cover
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
                                screenshots = screenshots.toMutableList(),
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
                            a.addEpisodesAll(episodesReady)
                            a.addAllMusic(music)
                            a.addAllAnimeGenre(g)
                            a.addAllAnimeStudios(st)
                            translations.forEach { translation ->
                                a.addTranslation(translation)
                            }
                            a.addMediaAll(media.filterNotNull())
                            animeRepository.save(a)
                        } else {
                            val a = animeRepository.findByShikimoriIdWithTranslation(anime.shikimoriId.toInt()).get()

                            Thread.sleep(1000)
                            val mediaTemp = runBlocking {
                                client.get {
                                    headers {
                                        contentType(ContentType.Application.Json)
                                    }
                                    url {
                                        protocol = URLProtocol.HTTPS
                                        host = "shikimori.me/api/animes/${anime.shikimoriId}"
                                    }
                                }.body<AnimeMediaParse>()
                            }

                            val translations: MutableList<AnimeTranslationTable> = mutableListOf()

                            mediaTemp.fandubbers.forEach Translations@{
                                val checkTrans = checkTranslation(it)
                                if (checkTrans.id == 0) return@Translations
                                val translationIs = animeTranslationRepository.findById(checkTrans.id).isPresent
                                translations.add(
                                    if (translationIs) {
                                        animeTranslationRepository.findById(checkTrans.id).get()
                                    } else {
                                        animeTranslationRepository.save(checkTrans)
                                    }
                                )
                            }

                            translations.forEach { translation ->
                                if (a.translation.find { animeTrans -> animeTrans.id == translation.id } == null)
                                    a.addTranslation(translation)
                            }
                        }
                    } catch (e: Exception) {
                        println("WAXZCF = ${e.message}")
                        return@Loop
                    }
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
            Thread.sleep(5000)
        }
        } catch (e: Exception) {
            println(e.message)
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
    suspend fun processEpisodes(urlLinking: String, kodikEpisodes: Map<String, Episode>, kitsuEpisodes: List<EpisodesKitsu>): List<AnimeEpisodeTable> {
        val episodeReady = mutableListOf<AnimeEpisodeTable>()

        val jobs = kodikEpisodes.map { (episodeKey, episode) ->
            val kitsuEpisode = findEpisodeByNumber(episodeKey.toInt(), kitsuEpisodes)
            CoroutineScope(Dispatchers.Default).async {
                processEpisode(urlLinking, episodeKey.toInt(), episode.link, kitsuEpisode)
            }
        }

        val processedEpisodes = jobs.awaitAll()
        episodeReady.addAll(processedEpisodes)

        return episodeReady
    }

    suspend fun processEpisode(
        urlLinking: String,
        episode: Int,
        link: String,
        kitsuEpisode: EpisodesKitsu?
    ): AnimeEpisodeTable {
        return if (kitsuEpisode != null) {
            val deferredTitle = CoroutineScope(Dispatchers.Default).async {
                translateText(kitsuEpisode.attributes?.titles?.enToUs ?: "")
            }

            val deferredDescription = CoroutineScope(Dispatchers.Default).async {
                translateText(kitsuEpisode.attributes?.description ?: "")
            }

            val translatedTitleEpisode = deferredTitle.await()
            val translatedDescriptionEpisode = deferredDescription.await()

            val imageEpisode = if(kitsuEpisode.attributes?.thumbnail != null) {
                imageService.saveFileInSThird(
                    "images/episodes/$urlLinking/$episode.png",
                    URL(kitsuEpisode.attributes.thumbnail.original).readBytes(),
                    compress = true,
                    width = 640,
                    height = 360
                )
            } else ""

            AnimeEpisodeTable(
                title = translatedTitleEpisode,
                titleEn = kitsuEpisode.attributes?.titles?.enToUs ?: "",
                description = translatedDescriptionEpisode,
                descriptionEn = kitsuEpisode.attributes?.description ?: "",
                link = link,
                number = kitsuEpisode.attributes?.number ?: episode,
                image = imageEpisode
            )
        } else {
            return AnimeEpisodeTable(
                title = episode.toString(),
                titleEn = episode.toString(),
                description = null,
                descriptionEn = null,
                link = link,
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

    suspend fun translateText(text: String): String {
        val translatedText = client.post {
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
            setBody(
                listOf(
                    TextMicRequest(text = text)
                )
            )
            header("Accept", "application/vnd.api+json")
            parameter("from", "en")
            parameter("to", "ru")
            parameter("api-version", "3.0")
        }.body<List<TextTranslations>>()

        return translatedText[0].translations[0].text ?: ""
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

    fun checkTranslation(element: String): AnimeTranslationTable {
        return when {
            element.lowercase(Locale.getDefault()).contains("shiza") -> {
                AnimeTranslationTable(
                    id = 767,
                    title = "SHIZA Project",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("jam") -> {
                AnimeTranslationTable(
                    id = 557,
                    title = "JAM",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("kansai") -> {
                AnimeTranslationTable(
                    id = 559,
                    title = "Kansai",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("anidub") -> {
                AnimeTranslationTable(
                    id = 609,
                    title = "AniDUB",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("anilibria") -> {
                animeTranslationRepository.save(
                    AnimeTranslationTable(
                        id = 610,
                        title = "AniLibria",
                        voice = "voice"
                    )
                )
            }

            element.lowercase(Locale.getDefault()).contains("wakanim") -> {
                AnimeTranslationTable(
                    id = 643,
                    title = "Studio Band",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("flarrow") -> {
                AnimeTranslationTable(
                    id = 643,
                    title = "Studio Band",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("studio band") -> {
                AnimeTranslationTable(
                    id = 643,
                    title = "Studio Band",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("2x2") -> {
                AnimeTranslationTable(
                    id = 735,
                    title = "2x2",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("animedia") -> {
                AnimeTranslationTable(
                    id = 739,
                    title = "Animedia",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("hdrezka") -> {
                AnimeTranslationTable(
                    id = 794,
                    title = "HDrezka Studio",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("animaunt") -> {
                AnimeTranslationTable(
                    id = 825,
                    title = "AniMaunt",
                    voice = "voice"
                )
            }

            element.lowercase(Locale.getDefault()).contains("amber") -> {
                AnimeTranslationTable(
                    id = 933,
                    title = "Amber",
                    voice = "voice"
                )
            }

            else -> {
                AnimeTranslationTable()
            }
        }
    }
}
