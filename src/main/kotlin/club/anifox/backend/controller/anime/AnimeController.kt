package club.anifox.backend.controller.anime

import club.anifox.backend.domain.enums.anime.AnimeOrder
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.light.AnimeLight
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
        @Schema(defaultValue = "0", name = "pageNum") pageNum: @Min(0) @Max(500) Int,
        @Schema(defaultValue = "48", name = "pageSize") pageSize: @Min(1) @Max(500) Int,
        @RequestParam(name = "genres", required = false)
        @Parameter(name = "genres", description = "Require genres IDS", required = false)
        genres: List<String>?,
        @Schema(name = "status", required = false, nullable = true) status: AnimeStatus?,
        @Schema(name = "order", required = false, nullable = true) order: AnimeOrder?,
        @Schema(name = "searchQuery", required = false, nullable = true) searchQuery: String?,
        @Schema(name = "season", required = false, nullable = true) season: AnimeSeason?,
        @Schema(name = "ratingMpa", required = false, nullable = true, description = "Must be one of: PG | PG-13 | R | R+ | G") ratingMpa: String?,
        @Schema(name = "minimalAge", required = false, nullable = true, description = "Must be one of: 18 | 16 | 12 | 6 | 0") minimalAge: Int?,
        @Schema(name = "type", required = false, nullable = true) type: AnimeType?,
        @Schema(name = "studio", required = false, nullable = true, description = "Anime studio made by") studio: String?,
        @RequestParam(name = "year", required = false)
        @Parameter(name = "year", description = "Require list of year", required = false)
        year: List<Int>?,
        @RequestParam(name = "translation", required = false)
        @Parameter(name = "translation", description = "Require translation IDS", required = false)
        translations: List<String>?,
    ): List<AnimeLight> {
        return animeService.getAnime(
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
            studio = studio,
        )
    }
}
