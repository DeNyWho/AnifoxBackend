package club.anifox.backend.controller.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeDefaultFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.character.AnimeCharacterResponse
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.episode.AnimeEpisode
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeHistory
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.sitemap.AnimeSitemap
import club.anifox.backend.domain.model.anime.statistics.AnimeStatistics
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.service.anime.AnimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@CrossOrigin("*")
@Tag(name = "AnimeAPI", description = "All about anime")
@RequestMapping("/api/anime")
class AnimeController {
    @Autowired
    lateinit var animeService: AnimeService

    @GetMapping
    @Operation(summary = "get all anime")
    fun getAnime(
        @RequestParam(required = true)
        @Schema(defaultValue = "0", name = "page")
        page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(required = true)
        @Schema(defaultValue = "48", name = "limit")
        limit:
        @Min(1)
        @Max(500)
        Int,
        @RequestParam(required = false)
        status: AnimeStatus?,
        @RequestParam(required = false)
        @Schema(description = "Enum: update, aired, released, random, rating")
        order: AnimeSearchFilter?,
        @RequestParam(required = false)
        @Schema(description = "Enum: desc, asc")
        sort: AnimeDefaultFilter?,
        @Schema(name = "search", required = false, nullable = true)
        search: String?,
        @RequestParam(required = false)
        @Schema(description = "Enum: winter, spring, summer, fall")
        season: AnimeSeason?,
        @RequestParam(required = false)
        @Schema(description = "Enum: movie, ona, ova, music, special, tv")
        type: AnimeType?,
        @RequestParam(name = "genres", required = false)
        @Parameter(name = "genres", description = "Require genres IDS", required = false)
        genres: List<String>?,
        @RequestParam(name = "rating_mpa", required = false)
        @Parameter(name = "rating_mpa", required = false, description = "Enum: PG | PG-13 | R | R+ | G")
        ratingMpa: String?,
        @RequestParam(name = "episode_count", required = false)
        @Parameter(name = "episode_count", required = false)
        episodeCount: Int?,
        @Schema(name = "age", required = false, nullable = true, description = "Must be one of: 18 | 16 | 12 | 6 | 0") age: Int?,
        @RequestParam(name = "studios", required = false)
        @Parameter(name = "studios", description = "Require studios IDS", required = false)
        studios: List<String>?,
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
            orderBy = order,
            sort = sort,
            searchQuery = search,
            season = season,
            ratingMpa = ratingMpa,
            minimalAge = age,
            type = type,
            year = year,
            translations = translations,
            studios = studios,
            episodeCount = episodeCount,
        )
    }

    @PostMapping("/block")
    fun addBlockedAnime(
        @RequestHeader(value = "Authorization") token: String,
        @RequestParam(required = false) url: String?,
        @RequestParam(required = false) shikimoriId: Int?,
    ) {
        animeService.addBlocked(url, shikimoriId)
    }

    @GetMapping("/{url}")
    @Operation(summary = "detail anime")
    fun getAnimeDetails(
        @RequestHeader(value = "Authorization", required = false) token: String?,
        @PathVariable url: String,
    ): AnimeDetail {
        return animeService.getAnimeDetails(token, url)
    }

    @GetMapping("/{url}/statistics")
    fun getAnimeStatistics(
        @PathVariable url: String,
    ): AnimeStatistics {
        return animeService.getAnimeStatistics(url)
    }

    @GetMapping("/{id}/external_links")
    fun getExternalLinks(@PathVariable id: String): List<AnimeExternalLinksTable> {
        return animeService.findExternalLinksByAnimeId(id)
    }

    @GetMapping("/{url}/similar")
    @Operation(summary = "similar anime")
    fun getAnimeSimilar(
        @PathVariable url: String,
        @RequestParam(required = true)
        @Schema(defaultValue = "0", name = "page")
        page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(required = true)
        @Schema(defaultValue = "48", name = "limit")
        limit:
        @Min(1)
        @Max(500)
        Int,
    ): List<AnimeLight> {
        return animeService.getAnimeSimilar(page, limit, url)
    }

    @GetMapping("/{url}/related")
    @Operation(summary = "related anime")
    fun getAnimeRelated(
        @PathVariable url: String,
    ): List<AnimeRelationLight> {
        return animeService.getAnimeRelated(url)
    }

    @GetMapping("/{url}/screenshots")
    @Operation(summary = "anime screenshots")
    fun getAnimeScreenshots(
        @PathVariable url: String,
    ): List<String> {
        return animeService.getAnimeScreenshots(url)
    }

    @GetMapping("/{url}/videos")
    @Operation(summary = "anime videos")
    fun getAnimeVideos(
        @PathVariable url: String,
        type: AnimeVideoType?,
    ): List<AnimeVideo> {
        return animeService.getAnimeVideos(url, type)
    }

    @GetMapping("/{url}/characters")
    @Operation(summary = "anime characters")
    fun getAnimeCharacters(
        @RequestParam(required = true)
        @Schema(defaultValue = "0", name = "page")
        page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(required = true)
        @Schema(defaultValue = "48", name = "limit")
        limit:
        @Min(1)
        @Max(500)
        Int,
        @PathVariable url: String,
        @RequestParam(name = "role", required = false)
        role: String?,
        @RequestParam(name = "search", required = false)
        search: String?,
    ): AnimeCharacterResponse {
        return animeService.getAnimeCharacters(page, limit, url, role, search)
    }

    @GetMapping("/{url}/episodes")
    @Operation(summary = "anime episodes")
    fun getAnimeEpisodes(
        @RequestHeader(value = "Authorization", required = false) token: String?,
        @PathVariable url: String,
        @RequestParam(defaultValue = "0", name = "page") page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
        @Min(1)
        @Max(500)
        Int,
        sort: AnimeDefaultFilter?,
        @RequestParam(name = "translation_id", required = false)
        translationId: Int?,
        @RequestParam(name = "search", required = false)
        searchQuery: String?,
    ): List<AnimeEpisode> {
        return animeService.getAnimeEpisodes(token, url, page, limit, sort, translationId, searchQuery)
    }

    @GetMapping("/{url}/episodes/history")
    @Operation(summary = "anime episodes history date")
    fun getAnimeEpisodesHistory(
        @PathVariable url: String,
        @RequestParam(defaultValue = "0", name = "page") page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
        @Min(1)
        @Max(500)
        Int,
    ): List<AnimeEpisodeHistory> {
        return animeService.getAnimeEpisodesHistory(url, page, limit)
    }

    @GetMapping("/schedules")
    @Operation(
        summary = "anime schedules",
        description = """
            Get anime schedules filtered by date range and/or day of week.
            Available days of week: monday, tuesday, wednesday, thursday, friday, saturday, sunday
        """,
    )
    fun getAnimeSchedules(
        @RequestParam(defaultValue = "0", name = "page") page:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
        @Min(1)
        @Max(500)
        Int,
        @RequestParam(name = "date", required = true)
        @Schema(description = "Format: dd-MM-yyyy")
        @DateTimeFormat(pattern = "dd-MM-yyyy")
        date: LocalDate,
    ): Map<String, List<AnimeLight>> {
        return animeService.getWeeklySchedule(page, limit, date)
    }

    @GetMapping("/years")
    @Operation(summary = "anime years")
    fun getAnimeYears(): List<String> {
        return animeService.getAnimeYears()
    }

    @GetMapping("/studios")
    @Operation(summary = "anime studios")
    fun getAnimeStudios(): List<AnimeStudio> {
        return animeService.getAnimeStudios()
    }

    @GetMapping("/genres")
    @Operation(summary = "anime genres")
    fun getAnimeGenres(): List<AnimeGenre> {
        return animeService.getAnimeGenres()
    }

    @GetMapping("/{url}/translations/count")
    @Operation(summary = "anime translations count")
    fun getAnimeTranslationCount(
        @PathVariable url: String,
    ): List<AnimeTranslationCount> {
        return animeService.getAnimeTranslationsCount(url)
    }

    @GetMapping("/translations")
    @Operation(summary = "anime translations")
    fun getAnimeTranslations(): List<AnimeTranslationTable> {
        return animeService.getAnimeTranslations()
    }

    @GetMapping("/sitemap")
    @Operation(summary = "anime site map")
    fun getAnimeSiteMap(): List<AnimeSitemap> {
        return animeService.getAnimeSitemap()
    }
}
