package club.anifox.backend.controller.users

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeProgressRequest
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.service.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin("*")
@Tag(name = "UsersAnimeApi", description = "All about user anime")
@RequestMapping("/api/users/anime/")
@PreAuthorize("hasRole('ROLE_USER')")
class UsersAnimeController(
    private val userService: UserService,
) {

    @PostMapping("{url}/favorite")
    fun addToFavoriteAnime(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @RequestParam status: StatusFavourite,
        @RequestParam(name = "episodes_watched", required = false)
        @Schema(name = "episodes_watched", required = false, nullable = true)
        episodesWatched: Int?,
        response: HttpServletResponse,
    ) {
        userService.addToFavoritesAnime(token, url, status, episodesWatched, response)
    }

    @GetMapping("favorite/{status}")
    fun getFavoriteAnimeByStatus(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable status: StatusFavourite,
        @RequestParam(defaultValue = "0", name = "page") page:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
            @Min(1)
            @Max(500)
            Int,
    ): List<AnimeLight> {
        return userService.getFavoritesAnimeByStatus(token, status, page, limit)
    }

    @GetMapping("recommendations")
    @Operation(summary = "recommendation anime")
    fun getAnimeRecommendations(
        @RequestHeader(value = "Authorization") token: String,
        @RequestParam(defaultValue = "0", name = "page") page:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
            @Min(1)
            @Max(500)
            Int,
    ): List<AnimeLight> {
        return userService.getRecommendations(token, page, limit)
    }

    @PostMapping("{url}/episode/{number}/progress")
    fun changeEpisodeProgress(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @PathVariable number: Int,
        @RequestBody progress: AnimeEpisodeProgressRequest,
    ) {
        userService.changeEpisodeProgress(token, url, number, progress)
    }

    @PostMapping("genres")
    fun updatePreferredGenres(
        @RequestHeader(value = "Authorization") token: String,
        @RequestBody genres: List<String>,
        response: HttpServletResponse,
    ) {
        userService.updatePreferredGenres(token, genres, response)
    }

    @GetMapping("recently")
    fun getRecentlyAnime(
        @RequestParam(defaultValue = "0", name = "page") page:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "limit") limit:
            @Min(1)
            @Max(500)
            Int,
        @RequestHeader(value = "Authorization") token: String,
        response: HttpServletResponse,
    ): List<AnimeLight> {
        return userService.getRecentlyAnimeAll(token, page, limit)
    }

    @PostMapping("{url}/rating")
    fun setAnimeRating(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        rating:
            @Min(0)
            @Max(10)
            Int,
        response: HttpServletResponse,
    ) {
        userService.setAnimeRating(token, url, rating, response)
    }
}
