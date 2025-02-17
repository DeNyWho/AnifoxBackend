package club.anifox.backend.service.user

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeProgressRequest
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.repository.user.UserRepository
import club.anifox.backend.service.user.component.UserAnimeInteractionsComponent
import club.anifox.backend.service.user.component.favourite.AnimeFavoriteStatusComponent
import club.anifox.backend.service.user.component.rating.AnimeRatingComponent
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAnimeInteractionsComponent: UserAnimeInteractionsComponent,
    private val animeRatingComponent: AnimeRatingComponent,
    private val animeFavoriteStatusService: AnimeFavoriteStatusComponent,
) : UserRepository {
    override fun addRating(
        token: String,
        url: String,
        rating: Int,
        response: HttpServletResponse,
    ) {
        animeRatingComponent.addRating(token = token, url = url, rating = rating, response = response)
    }

    override fun changeEpisodeProgress(
        token: String,
        url: String,
        episodeNumber: Int,
        progress: AnimeEpisodeProgressRequest,
    ) {
        userAnimeInteractionsComponent.changeEpisodeProgress(token, url, episodeNumber, progress)
    }

    override fun getRecentlyAnimeAll(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        return userAnimeInteractionsComponent.getRecentlyAnimeList(token = token, page = page, limit = limit)
    }

    override fun updatePreferredGenres(
        token: String,
        genres: List<String>,
        response: HttpServletResponse,
    ) {
        return userAnimeInteractionsComponent.updatePreferredGenres(token = token, genres = genres, response = response)
    }

    override fun getRecommendations(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        return userAnimeInteractionsComponent.getRecommendations(token = token, page = page, limit = limit)
    }

    override fun addToFavoritesAnime(
        token: String,
        url: String,
        status: StatusFavourite,
        episodesWatched: Int?,
        response: HttpServletResponse,
    ) {
        animeFavoriteStatusService.addToFavorites(token = token, url = url, status = status, episodesWatched = episodesWatched, response = response)
    }

    override fun getFavoritesAnimeByStatus(
        token: String,
        status: StatusFavourite,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        return animeFavoriteStatusService.getFavoritesByStatus(token = token, status = status, page = page, limit = limit)
    }
}
