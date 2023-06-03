package com.example.backend.controller.user

import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.mangaResponse.light.MangaLight
import com.example.backend.models.users.*
import com.example.backend.service.user.UserService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletResponse
import javax.validation.constraints.Max
import javax.validation.constraints.Min


@RestController
@CrossOrigin("*")
@Tag(name = "UsersApi", description = "All about users")
@RequestMapping("/api/users")
class UsersController(
    private val userService: UserService
) {
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/anime/favorite")
    fun addToFavoriteAnime(
        @RequestHeader (value = "Authorization") token: String,
        @RequestParam url: String,
        @RequestParam status: StatusFavourite,
        response: HttpServletResponse
    ) {
        userService.addToFavoritesAnime(token, url, status, response)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/manga/favorite")
    fun addToFavoriteManga(
        @RequestHeader (value = "Authorization") token: String,
        @RequestParam id: String,
        @RequestParam status: StatusFavourite,
        response: HttpServletResponse
    ) {
        userService.addToFavoritesManga(token, id, status, response)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/anime/favorite/{status}")
    fun getFavoriteAnimeByStatus(
        @RequestHeader (value = "Authorization") token: String,
        @PathVariable status: StatusFavourite,
        @RequestParam(defaultValue = "0", name = "pageNum")  pageNum: @Min(0) @Max(500) Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize: @Min(1) @Max(500) Int
    ): List<AnimeLight> {
        return userService.getFavoritesAnimeByStatus(token, status, pageNum, pageSize)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/manga/favorite/{status}")
    fun getFavoriteMangaByStatus(
        @RequestHeader (value = "Authorization") token: String,
        @PathVariable status: StatusFavourite,
        @RequestParam(defaultValue = "0", name = "pageNum")  pageNum: @Min(0) @Max(500) Int,
        @RequestParam(defaultValue = "48", name = "pageSize") pageSize: @Min(1) @Max(500) Int
    ): List<MangaLight> {
        return userService.getFavoritesMangaByStatus(token, status, pageNum, pageSize)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/whoami")
    fun whoAmi(
        @RequestHeader (value = "Authorization") token: String,
    ): WhoAmi {
        return userService.whoAmi(token)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/anime/{url}/rating")
    fun setAnimeRating(
        @RequestHeader (value = "Authorization") token: String,
        @PathVariable url: String,
        rating: @Min(0) @Max(10) Int,
        response: HttpServletResponse
    ) {
        userService.setAnimeRating(token, url, rating, response)
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/manga/{id}/rating")
    fun setMangaRating(
        @RequestHeader (value = "Authorization") token: String,
        @PathVariable id: String,
        rating: @Min(0) @Max(10) Int,
        response: HttpServletResponse
    ) {
        userService.setMangaRating(token, id, rating, response)
    }
}