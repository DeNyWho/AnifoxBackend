package club.anifox.backend.controller.users

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.recently.AnimeRecently
import club.anifox.backend.domain.model.anime.recently.AnimeRecentlyRequest
import club.anifox.backend.service.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
class UsersAnimeController(
    private val userService: UserService,
) {

    @PostMapping("{url}/favorite")
    fun addToFavoriteAnime(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @RequestParam status: StatusFavourite,
        @Schema(name = "episodeNumber", required = false, nullable = true) episodeNumber: Int?,
        response: HttpServletResponse,
    ) {
        userService.addToFavoritesAnime(token, url, status, episodeNumber, response)
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

    @PostMapping("{url}/recently")
    fun addToRecentlyAnime(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @RequestBody recently: AnimeRecentlyRequest,
        response: HttpServletResponse,
    ) {
        userService.addToRecentlyAnime(token, url, recently, response)
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
    ): List<AnimeRecently> {
        return userService.getRecentlyAnimeAll(token, page, limit)
    }

    @GetMapping("{url}/recently")
    fun getRecentlyAnimeByUrl(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        response: HttpServletResponse,
    ): AnimeRecently {
        return userService.getRecentlyAnimeByUrl(token, url)
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
