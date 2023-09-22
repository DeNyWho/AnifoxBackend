package club.anifox.backend.service.user

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.recently.AnimeRecently
import club.anifox.backend.domain.model.anime.recently.AnimeRecentlyRequest
import club.anifox.backend.domain.repository.user.UserRepository
import club.anifox.backend.service.user.component.UserAnimeInteractionsComponent
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userAnimeInteractionsComponent: UserAnimeInteractionsComponent,
) : UserRepository {

    override fun setAnimeRating(token: String, url: String, rating: Int, response: HttpServletResponse) {
        userAnimeInteractionsComponent.setRating(token = token, url = url, rating = rating, response = response)
    }

    override fun addToRecentlyAnime(token: String, url: String, recently: AnimeRecentlyRequest, response: HttpServletResponse) {
        return userAnimeInteractionsComponent.addRecently(token = token, url = url, recently = recently, response = response)
    }

    override fun getRecentlyAnimeByUrl(token: String, url: String): AnimeRecently {
        return userAnimeInteractionsComponent.getRecentlyByUrl(token = token, url = url)
    }

    override fun getRecentlyAnimeAll(token: String, pageNum: Int, pageSize: Int): List<AnimeRecently> {
        return userAnimeInteractionsComponent.getRecentlyAnimeList(token = token, pageNum = pageNum, pageSize = pageSize)
    }

    override fun addToFavoritesAnime(token: String, url: String, status: StatusFavourite, episodeNumber: Int?, response: HttpServletResponse) {
        userAnimeInteractionsComponent.addToFavoritesAnime(token = token, url = url, status = status, episodeNumber = episodeNumber, response = response)
    }

    override fun getFavoritesAnimeByStatus(token: String, status: StatusFavourite, pageNum: Int, pageSize: Int): List<AnimeLight> {
        return userAnimeInteractionsComponent.getFavoritesByStatus(token = token, status = status, pageNum = pageNum, pageSize = pageSize)
    }
}
