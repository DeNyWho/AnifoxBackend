package club.anifox.backend.controller.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.filter.AnimeEpisodeFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeMedia
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable
import club.anifox.backend.service.anime.AnimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin("*")
@Tag(name = "AnimeAPI", description = "All about anime")
@RequestMapping("/api/anime/")
class AnimeController {

    @Autowired
    lateinit var animeService: AnimeService

    @GetMapping
    @Operation(summary = "get all anime")
    fun getAnime(
        @Schema(defaultValue = "0", name = "page")
        page:
            @Min(0)
            @Max(500)
            Int,
        @Schema(defaultValue = "48", name = "limit")
        limit:
            @Min(1)
            @Max(500)
            Int,
        @RequestParam(name = "genres", required = false)
        @Parameter(name = "genres", description = "Require genres IDS", required = false)
        genres: List<String>?,
        status: AnimeStatus?,
        filter: AnimeSearchFilter?,
        @Schema(name = "search", required = false, nullable = true) search: String?,
        season: AnimeSeason?,
        @Schema(name = "rating", required = false, nullable = true, description = "Must be one of: PG | PG-13 | R | R+ | G") rating: String?,
        @Schema(name = "age", required = false, nullable = true, description = "Must be one of: 18 | 16 | 12 | 6 | 0") age: Int?,
        type: AnimeType?,
        @Schema(name = "studio", required = false, nullable = true, description = "Anime studio made by") studio: String?,
        @RequestParam(name = "year", required = false)
        @Parameter(name = "year", description = "Require list of year", required = false)
        year: List<Int>?,
        @RequestParam(name = "translation", required = false)
        @Parameter(name = "translation", description = "Require translation IDS", required = false)
        translations: List<String>?,
    ): List<AnimeLight> {
        return animeService.getAnime(
            page = page,
            limit = limit,
            genres = genres,
            status = status,
            filter = filter,
            searchQuery = search,
            season = season,
            ratingMpa = rating,
            minimalAge = age,
            type = type,
            year = year,
            translations = translations,
            studio = studio,
        )
    }

    @GetMapping("{url}")
    @Operation(summary = "detail anime")
    fun getAnimeDetails(
        @PathVariable url: String,
    ): AnimeDetail {
        return animeService.getAnimeDetails(url)
    }

    @GetMapping("{url}/similar")
    @Operation(summary = "similar anime")
    fun getAnimeSimilar(
        @PathVariable url: String,
    ): List<AnimeLight> {
        return animeService.getAnimeSimilar(url)
    }

    @GetMapping("{url}/related")
    @Operation(summary = "related anime")
    fun getAnimeRelated(
        @PathVariable url: String,
    ): List<AnimeRelationLight> {
        return animeService.getAnimeRelated(url)
    }

    @GetMapping("{url}/screenshots")
    @Operation(summary = "anime screenshots")
    fun getAnimeScreenshots(
        @PathVariable url: String,
    ): List<String> {
        return animeService.getAnimeScreenshots(url)
    }

    @GetMapping("{url}/media")
    @Operation(summary = "anime media")
    fun getAnimeMedia(
        @PathVariable url: String,
    ): List<AnimeMedia> {
        return animeService.getAnimeMedia(url)
    }

    @GetMapping("{url}/episodes")
    @Operation(summary = "anime episodes")
    fun getAnimeEpisodes(
        @PathVariable url: String,
        @RequestParam(defaultValue = "0", name = "page") page:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
            @Min(1)
            @Max(500)
            Int,
        sort: AnimeEpisodeFilter?,
    ): List<AnimeEpisodeLight> {
        return animeService.getAnimeEpisodes(url, page, limit, sort)
    }

    @GetMapping("years")
    @Operation(summary = "anime years")
    fun getAnimeYears(): List<String> {
        return animeService.getAnimeYears()
    }

    @GetMapping("studios")
    @Operation(summary = "anime studios")
    fun getAnimeStudios(): List<AnimeStudio> {
        return animeService.getAnimeStudios()
    }

    @GetMapping("genres")
    @Operation(summary = "anime genres")
    fun getAnimeGenres(): List<AnimeGenre> {
        return animeService.getAnimeGenres()
    }

    @GetMapping("{url}/translations/count")
    @Operation(summary = "anime translations count")
    fun getAnimeTranslationCount(
        @PathVariable url: String,
    ): List<AnimeTranslationCount> {
        return animeService.getAnimeTranslationsCount(url)
    }

    @GetMapping("translations")
    @Operation(summary = "anime translations")
    fun getAnimeTranslations(): List<AnimeTranslationTable> {
        return animeService.getAnimeTranslations()
    }
}
