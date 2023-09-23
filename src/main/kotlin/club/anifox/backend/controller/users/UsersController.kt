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
@Tag(name = "UsersApi", description = "All about users")
@RequestMapping("/api/users/")
class UsersController(
    private val userService: UserService,
) {
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("anime/{url}/favorite")
    fun addToFavoriteAnime(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @RequestParam status: StatusFavourite,
        @Schema(name = "episodeNumber", required = false, nullable = true) episodeNumber: Int?,
        response: HttpServletResponse,
    ) {
        userService.addToFavoritesAnime(token, url, status, episodeNumber, response)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("anime/favorite/{status}")
    fun getFavoriteAnimeByStatus(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable status: StatusFavourite,
        @RequestParam(defaultValue = "0", name = "pageNum") pageNum:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize:
            @Min(1)
            @Max(500)
            Int,
    ): List<AnimeLight> {
        return userService.getFavoritesAnimeByStatus(token, status, pageNum, pageSize)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("recommendations")
    @Operation(summary = "recommendation anime")
    fun getAnimeRecommendations(
        @RequestHeader(value = "Authorization") token: String,
        @RequestParam(defaultValue = "0", name = "pageNum") pageNum:
        @Min(0)
        @Max(500)
        Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize:
        @Min(1)
        @Max(500)
        Int,
    ): List<AnimeLight> {
        return userService.getRecommendations(token, pageNum, pageSize)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("anime/{url}/recently")
    fun addToRecentlyAnime(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        @RequestBody recently: AnimeRecentlyRequest,
        response: HttpServletResponse,
    ) {
        userService.addToRecentlyAnime(token, url, recently, response)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("anime/recently")
    fun getRecentlyAnime(
        @RequestParam(defaultValue = "0", name = "pageNum") pageNum:
            @Min(0)
            @Max(500)
            Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize:
            @Min(1)
            @Max(500)
            Int,
        @RequestHeader(value = "Authorization") token: String,
        response: HttpServletResponse,
    ): List<AnimeRecently> {
        return userService.getRecentlyAnimeAll(token, pageNum, pageSize)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("anime/{url}/recently")
    fun getRecentlyAnimeByUrl(
        @RequestHeader(value = "Authorization") token: String,
        @PathVariable url: String,
        response: HttpServletResponse,
    ): AnimeRecently {
        return userService.getRecentlyAnimeByUrl(token, url)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("anime/{url}/rating")
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
