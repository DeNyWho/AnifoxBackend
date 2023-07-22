package com.example.backend.repository.user

import com.example.backend.models.animeRequest.RecentlyRequest
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.user.RecentlyAnimeLight
import com.example.backend.models.mangaResponse.light.MangaLight
import com.example.backend.models.users.StatusFavourite
import com.example.backend.models.users.WhoAmi
import org.springframework.stereotype.Repository
import javax.servlet.http.HttpServletResponse

@Repository
interface UserRepositoryImpl {

    fun addToFavoritesAnime(token: String, url: String, status: StatusFavourite, episodeNumber: Int?, response: HttpServletResponse)
    fun getFavoritesAnimeByStatus(token: String, status: StatusFavourite, pageNum: Int, pageSize: Int): List<AnimeLight>
    fun setAnimeRating(token: String, url: String, rating: Int, response: HttpServletResponse)
    fun whoAmi(token: String): WhoAmi
    fun addToFavoritesManga(token: String, id: String, status: StatusFavourite, response: HttpServletResponse)
    fun getFavoritesMangaByStatus(token: String, status: StatusFavourite, pageNum: Int, pageSize: Int): List<MangaLight>
    fun setMangaRating(token: String, id: String, rating: Int, response: HttpServletResponse)
    fun addToRecentlyAnime(token: String, url: String, recently: RecentlyRequest, response: HttpServletResponse)
    fun getRecentlyAnimeList(token: String, pageNum: Int, pageSize: Int): List<RecentlyAnimeLight>

}