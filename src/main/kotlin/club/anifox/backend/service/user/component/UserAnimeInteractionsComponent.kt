package club.anifox.backend.service.user.component

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.mappers.anime.recently.toRecently
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.recently.AnimeRecently
import club.anifox.backend.domain.model.anime.recently.AnimeRecentlyRequest
import club.anifox.backend.jpa.entity.anime.AnimeRatingCountTable
import club.anifox.backend.jpa.entity.anime.AnimeRatingTable
import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import club.anifox.backend.jpa.entity.user.UserRecentlyAnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.user.anime.UserFavoriteAnimeRepository
import club.anifox.backend.jpa.repository.user.anime.UserRatingCountRepository
import club.anifox.backend.jpa.repository.user.anime.UserRatingRepository
import club.anifox.backend.jpa.repository.user.anime.UserRecentlyRepository
import club.anifox.backend.util.AnimeUtils
import club.anifox.backend.util.UserUtils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class UserAnimeInteractionsComponent {

    @Autowired
    private lateinit var userRatingCountRepository: UserRatingCountRepository

    @Autowired
    private lateinit var userRatingRepository: UserRatingRepository

    @Autowired
    private lateinit var userRecentlyRepository: UserRecentlyRepository

    @Autowired
    private lateinit var userFavoriteAnimeRepository: UserFavoriteAnimeRepository

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var userUtils: UserUtils

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    fun addToFavoritesAnime(
        token: String,
        url: String,
        status: StatusFavourite,
        episodeNumber: Int?,
        response: HttpServletResponse,
    ) {
        val user = userUtils.checkUser(token)
        val anime = animeUtils.checkAnime(url)
        val episode = if (episodeNumber != null) animeUtils.checkEpisode(url, episodeNumber) else null

        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)
        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()
            if (existFavorite.status == status && existFavorite.episode != null && existFavorite.episode?.number == episodeNumber) {
                return
            } else {
                existFavorite.status = status
                if (episode != null && existFavorite.episode != episode) {
                    existFavorite.episode = episode
                }

                userFavoriteAnimeRepository.save(existFavorite)
                return
            }
        }

        userFavoriteAnimeRepository.save(UserFavoriteAnimeTable(user = user, anime = anime, status = status, episode = episode))
        response.status = HttpStatus.CREATED.value()
    }

    fun getFavoritesByStatus(
        token: String,
        status: StatusFavourite,
        pageNum: Int,
        pageSize: Int,
    ): List<AnimeLight> {
        val user = userUtils.checkUser(token)
        val favoriteAnime = userFavoriteAnimeRepository.findByUserAndStatus(user, status, PageRequest.of(pageNum, pageSize))

        if (favoriteAnime.isNotEmpty()) {
            return favoriteAnime.map { it.anime.toAnimeLight() }
        }

        throw NotFoundException("The user has no recent anime")
    }

    fun setRating(
        token: String,
        url: String,
        rating: Int,
        response: HttpServletResponse,
    ) {
        val user = userUtils.checkUser(token)
        val anime = animeUtils.checkAnime(url)

        val existingRatingCount = userRatingCountRepository.findByAnimeAndRating(anime, rating)

        val existingRating = userRatingRepository.findByUserAndAnime(user, anime)
        if (existingRating.isPresent) {
            val existRating = existingRating.get()
            if (existRating.rating == rating) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                val prevRatingCount = userRatingCountRepository.findByAnimeAndRating(anime, existRating.rating).get()
                prevRatingCount.count--
                if (prevRatingCount.count == 0) {
                    userRatingCountRepository.deleteById(prevRatingCount.id)
                } else {
                    userRatingCountRepository.save(prevRatingCount)
                }

                existRating.rating = rating
                userRatingRepository.save(existRating)
                response.status = HttpStatus.OK.value()
            }
        } else {
            userRatingRepository.save(AnimeRatingTable(anime = anime, rating = rating, user = user))
            response.status = HttpStatus.CREATED.value()
        }

        if (existingRatingCount.isPresent) {
            val existCount = existingRatingCount.get()
            val a = existCount.count + 1
            existCount.count = a
            userRatingCountRepository.save(existCount)
        } else {
            userRatingCountRepository.save(AnimeRatingCountTable(anime = anime, rating = rating, count = 1))
        }

        val animeRating = userRatingRepository.findByAnime(anime)

        val totalRating = animeRating.map { it.rating }.average()
        anime.totalRating = totalRating
        animeRepository.save(anime)
    }

    fun addRecently(token: String, url: String, recently: AnimeRecentlyRequest, response: HttpServletResponse) {
        val user = userUtils.checkUser(token)
        val anime = animeUtils.checkAnime(url)
        val episode = animeUtils.checkEpisode(url, recently.episodeNumber)
        val translation = episode.translations.find { it.translation.id == recently.translationId } ?: throw NotFoundException("Translation not found")

        val existingRecently = userRecentlyRepository.findByUserTableAndAnime(user, anime)
        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)

        val favoriteAnime = existingFavorite.orElse(null)

        if (favoriteAnime == null) {
            userFavoriteAnimeRepository.save(
                UserFavoriteAnimeTable(
                    user = user,
                    anime = anime,
                    status = if (anime.episodesCount == episode.number && recently.timingInSeconds > 1000) {
                        StatusFavourite.Watched
                    } else {
                        StatusFavourite.Watching
                    },
                    episode = episode,
                ),
            )
        } else {
            favoriteAnime.episode = episode

            when (favoriteAnime.status) {
                StatusFavourite.Watching -> {
                    if (anime.episodesCount == episode.number && recently.timingInSeconds > 1000) {
                        favoriteAnime.status = StatusFavourite.Watched
                    } else {
                        favoriteAnime.status = StatusFavourite.Watching
                    }
                }
                StatusFavourite.InPlan, StatusFavourite.Postponed, StatusFavourite.Watched -> {
                    favoriteAnime.status = StatusFavourite.Watching
                }
            }

            userFavoriteAnimeRepository.save(favoriteAnime)
        }

        if (existingRecently.isPresent) {
            val existRecently = existingRecently.get()
            if (existRecently.date == recently.date && recently.timingInSeconds == existRecently.timingInSeconds && existRecently.episode == episode && existRecently.selectedTranslation.translation.id == recently.translationId) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                existRecently.date = recently.date
                existRecently.timingInSeconds = recently.timingInSeconds
                existRecently.episode = episode
                existRecently.selectedTranslation = translation

                userRecentlyRepository.save(existRecently)
                response.status = HttpStatus.OK.value()
                return
            }
        }

        userRecentlyRepository.save(
            UserRecentlyAnimeTable(
                userTable = user,
                anime = anime,
                timingInSeconds = recently.timingInSeconds,
                date = recently.date,
                episode = episode,
                selectedTranslation = translation,
            ),
        )
        response.status = HttpStatus.CREATED.value()
    }

    fun getRecentlyByUrl(
        token: String,
        url: String,
    ): AnimeRecently {
        val anime = animeUtils.checkAnime(url)
        val user = userUtils.checkUser(token)
        val recently = userRecentlyRepository.findByUserTableAndAnime(user, anime)
        if (recently.isPresent) {
            return recently.get().toRecently()
        }

        throw NotFoundException("Recently not found")
    }

    fun getRecentlyAnimeList(
        token: String,
        pageNum: Int,
        pageSize: Int,
    ): List<AnimeRecently> {
        val user = userUtils.checkUser(token)
        val recently = userRecentlyRepository.findByUserTable(user, PageRequest.of(pageNum, pageSize))

        if (recently.isNotEmpty()) {
            return recently.map { it.toRecently() }
        }

        throw NotFoundException("The user has no recent anime")
    }
}
