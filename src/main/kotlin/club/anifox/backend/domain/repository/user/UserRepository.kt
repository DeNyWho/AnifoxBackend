package club.anifox.backend.domain.repository.user

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeProgressRequest
import club.anifox.backend.domain.model.anime.light.AnimeLight
import jakarta.servlet.http.HttpServletResponse

interface UserRepository {
    fun addRating(
        token: String,
        url: String,
        rating: Int,
        response: HttpServletResponse,
    )

    fun getRecently(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight>

    fun addToFavorites(
        token: String,
        url: String,
        status: StatusFavourite,
        episodesWatched: Int?,
        response: HttpServletResponse,
    )

    fun getFavoritesByStatus(
        token: String,
        status: StatusFavourite,
        page: Int,
        limit: Int,
    ): List<AnimeLight>

    fun getRecommendations(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight>

    fun updatePreferredGenres(
        token: String,
        genres: List<String>,
        response: HttpServletResponse,
    )

    fun changeEpisodeProgress(
        token: String,
        url: String,
        episodeNumber: Int,
        progress: AnimeEpisodeProgressRequest,
    )

    fun deleteFavorite(token: String, url: String)
    fun deleteRating(token: String, url: String)
}
