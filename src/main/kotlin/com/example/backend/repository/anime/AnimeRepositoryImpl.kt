package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeGenreTable
import com.example.backend.jpa.anime.AnimeStudiosTable
import com.example.backend.jpa.anime.AnimeTranslationTable
import com.example.backend.models.ServiceResponse
import com.example.backend.models.animeResponse.detail.AnimeDetail
import com.example.backend.models.animeResponse.episode.EpisodeLight
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.light.AnimeLightWithType
import com.example.backend.models.animeResponse.media.AnimeMediaResponse
import com.example.backend.models.users.StatusFavourite
import org.springframework.stereotype.Repository

@Repository
interface AnimeRepositoryImpl {

    fun addDataToDB(translationID: String)
    fun getAnime(
        pageNum: Int,
        pageSize: Int,
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
    ): ServiceResponse<AnimeLight>

    fun getAnimeGenres(): ServiceResponse<AnimeGenreTable>
    fun getAnimeStudios(): ServiceResponse<AnimeStudiosTable>
    fun getAnimeTranslations(): ServiceResponse<AnimeTranslationTable>
    fun getAnimeYears(): ServiceResponse<String>
    fun getAnimeById(id: String): ServiceResponse<AnimeDetail>
    fun getAnimeScreenshotsById(id: String): ServiceResponse<String>
    fun getAnimeMediaById(id: String): ServiceResponse<AnimeMediaResponse>
    fun getAnimeUsersStatusCount(url: String): MutableMap<StatusFavourite, Long>
    fun getAnimeRelated(url: String): ServiceResponse<AnimeLightWithType>
    fun getAnimeSimilar(url: String): ServiceResponse<AnimeLight>
    fun getAnimeRating(url: String): Any
    fun getAnimeEpisodesWithPaging(url: String, pageNumber: Int, pageSize: Int): List<EpisodeLight>
    fun getAnimeEpisodeByNumberAndAnime(url: String, number: Int): AnimeEpisodeTable
}