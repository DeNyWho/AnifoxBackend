package com.example.backend.controller.anime

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeGenreTable
import com.example.backend.jpa.anime.AnimeStudiosTable
import com.example.backend.jpa.anime.AnimeTranslationTable
import com.example.backend.models.ServiceResponse
import com.example.backend.models.animeResponse.common.RatingResponse
import com.example.backend.models.animeResponse.detail.AnimeDetail
import com.example.backend.models.animeResponse.episode.EpisodeLight
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.light.AnimeLightWithType
import com.example.backend.models.animeResponse.media.AnimeMediaResponse
import com.example.backend.models.users.StatusFavourite
import com.example.backend.service.anime.AnimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.ws.rs.NotFoundException


@RestController
@CrossOrigin("*")
@Tag(name = "AnimeApi", description = "All about anime")
@RequestMapping("/api/anime/")
class AnimeController {

    @Autowired
    lateinit var animeService: AnimeService

    @GetMapping
    @Operation(summary = "get all anime")
    fun getAnime(
        @RequestParam(defaultValue = "0", name = "pageNum")  pageNum: @Min(0) @Max(500) Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize: @Min(1) @Max(500) Int,
        @RequestParam(name = "genres", required = false)
        @Parameter(name = "genres", description = "Require genres IDS", required = false)
        genres: List<String>?,
        @Schema(name = "status", required = false, description = "Must be one of: released | ongoing", nullable = true) status: String?,
        @Schema(name = "order", required = false, description = "Must be one of: random | popular | views", nullable = true) order: String?,
        @Schema(name = "searchQuery", required = false, nullable = true) searchQuery: String?,
        @Schema(name = "season", required = false, nullable = true, description = "Must be one of: Winter | Spring | Summer | Fall") season: String?,
        @Schema(name = "ratingMpa", required = false, nullable = true, description = "Must be one of: PG | PG-13 | R | R+ | G") ratingMpa: String?,
        @Schema(name = "minimalAge", required = false, nullable = true, description = "Must be one of: 18 | 16 | 12 | 6 | 0") minimalAge: Int?,
        @Schema(name = "type", required = false, nullable = true, description = "Must be one of: movie | ona | ova | music | special | tv") type: String?,
        @Schema(name = "studio", required = false, nullable = true, description = "Anime studio made by") studio: String?,
        @RequestParam(name = "year", required = false)
        @Parameter(name = "year", description = "Require list of year", required = false)
        year: List<Int>?,
        @RequestParam(name = "translation", required = false)
        @Parameter(name = "translation", description = "Require translation IDS", required = false)
        translations: List<String>?
    ): ServiceResponse<AnimeLight>? {
        return try {
            animeService.getAnime(
                pageNum = pageNum,
                pageSize = pageSize,
                genres = genres,
                status = status,
                order = order,
                searchQuery = searchQuery,
                season = season,
                ratingMpa = ratingMpa,
                minimalAge = minimalAge,
                type = type,
                year = year,
                translations = translations,
                studio = studio
            )
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}/related")
    @Operation(summary = "anime related")
    fun getAnimeRelated(
        @PathVariable url: String
    ): ServiceResponse<AnimeLightWithType> {
        return try {
            return animeService.getAnimeRelated(url)
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}/rating")
    @Operation(summary = "anime rating")
    fun getAnimeRating(
        @PathVariable url: String
    ): List<RatingResponse> {
        return animeService.getAnimeRating(url)
    }

    @GetMapping("{url}/similar")
    @Operation(summary = "anime similar")
    fun getAnimeSimilar(
        @PathVariable url: String
    ): ServiceResponse<AnimeLight> {
        return try {
            return animeService.getAnimeSimilar(url)
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}")
    @Operation(summary = "detail anime query")
    fun getAnimeDetails(
        @PathVariable url: String
    ): ServiceResponse<AnimeDetail>? {
        return try {
            return animeService.getAnimeById(url)
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}/status")
    @Operation(summary = "detail anime query")
    fun getAnimeStatusCount(
        @PathVariable url: String
    ): MutableMap<StatusFavourite, Long> {
        return animeService.getAnimeUsersStatusCount(url)
    }

    @GetMapping("{url}/screenshots")
    @Operation(summary = "anime screenshots")
    fun getAnimeScreenshots(
        @PathVariable url: String
    ): ServiceResponse<String>? {
        return try {
            animeService.getAnimeScreenshotsById(url)
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}/media")
    @Operation(summary = "anime media")
    fun getAnimeMedia(
        @PathVariable url: String
    ): ServiceResponse<AnimeMediaResponse>? {
        return try {
            animeService.getAnimeMediaById(url)
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("{url}/episodes")
    @Operation(summary = "anime episodes")
    fun getAnimeEpisodes(
        @PathVariable url: String,
        @RequestParam(defaultValue = "0", name = "pageNum")  pageNum: @Min(0) @Max(500) Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize: @Min(1) @Max(500) Int,
    ): List<EpisodeLight> {
        return try {
            animeService.getAnimeEpisodesWithPaging(url, pageNum, pageSize)
        } catch (e: ChangeSetPersister.NotFoundException) {
            throw NotFoundException(e.message)
        }
    }

    @GetMapping("{url}/episodes/{number}")
    @Operation(summary = "anime episodes")
    fun getAnimeEpisodes(
        @PathVariable url: String,
        @PathVariable number: Int,
    ): AnimeEpisodeTable {
        return try {
            animeService.getAnimeEpisodeByNumberAndAnime(url, number)
        } catch (e: ChangeSetPersister.NotFoundException) {
            throw NotFoundException(e.message)
        }
    }

    @GetMapping("genres")
    @Operation(summary = "get all anime genres")
    fun getAnimeGenres(): ServiceResponse<AnimeGenreTable>? {
        return try {
            animeService.getAnimeGenres()
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("years")
    @Operation(summary = "get all anime years")
    fun getAnimeYears(): ServiceResponse<String>? {
        return try {
            animeService.getAnimeYears()
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("studios")
    @Operation(summary = "get all anime studios")
    fun getAnimeStudios(): ServiceResponse<AnimeStudiosTable>? {
        return try {
            animeService.getAnimeStudios()
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

    @GetMapping("translations")
    @Operation(summary = "get all anime translations")
    fun getAnimeTranslations(): ServiceResponse<AnimeTranslationTable>? {
        return try {
            animeService.getAnimeTranslations()
        } catch (e: ChangeSetPersister.NotFoundException) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        }
    }

}