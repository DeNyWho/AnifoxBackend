package com.example.backend.service.user

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeRating
import com.example.backend.jpa.anime.AnimeRatingCount
import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.manga.MangaRating
import com.example.backend.jpa.manga.MangaRatingCount
import com.example.backend.jpa.manga.MangaTable
import com.example.backend.jpa.user.User
import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.jpa.user.UserFavouriteManga
import com.example.backend.jpa.user.UserRecentlyAnime
import com.example.backend.models.animeRequest.RecentlyRequest
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.animeResponse.user.RecentlyAnimeLight
import com.example.backend.models.mangaResponse.light.MangaLight
import com.example.backend.models.users.StatusFavourite
import com.example.backend.models.users.WhoAmi
import com.example.backend.repository.anime.AnimeRepository
import com.example.backend.repository.manga.MangaRepository
import com.example.backend.repository.user.*
import com.example.backend.repository.user.anime.UserFavoriteAnimeRepository
import com.example.backend.repository.user.anime.UserRatingCountRepository
import com.example.backend.repository.user.anime.UserRatingRepository
import com.example.backend.repository.user.anime.UserRecentlyRepository
import com.example.backend.repository.user.manga.UserFavouriteMangaRepository
import com.example.backend.repository.user.manga.UserMangaRatingRepository
import com.example.backend.repository.user.manga.UserRatingCountMangaRepository
import com.example.backend.util.TokenHelper
import com.example.backend.util.common.animeTableToAnimeLight
import com.example.backend.util.common.episodeToEpisodeLight
import com.example.backend.util.common.listToAnimeLight
import com.example.backend.util.common.listToMangaLight
import com.example.backend.util.exceptions.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Root
import javax.servlet.http.HttpServletResponse

@Service
class UserService : UserRepositoryImpl {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var mangaRepository: MangaRepository

    @Autowired
    private lateinit var userFavoriteAnimeRepository: UserFavoriteAnimeRepository

    @Autowired
    private lateinit var userRecentlyRepository: UserRecentlyRepository

    @Autowired
    private lateinit var userFavoriteMangaRepository: UserFavouriteMangaRepository

    @Autowired
    private lateinit var userRatingRepository: UserRatingRepository

    @Autowired
    private lateinit var userMangaRatingRepository: UserMangaRatingRepository

    @Autowired
    private lateinit var userRatingCountRepository: UserRatingCountRepository

    @Autowired
    private lateinit var userRatingCountMangaRepository: UserRatingCountMangaRepository

    @Autowired
    private lateinit var tokenHelper: TokenHelper

    override fun addToFavoritesAnime(
        token: String,
        url: String,
        status: StatusFavourite,
        episodeNumber: Int?,
        response: HttpServletResponse
    ) {
        val user = checkUser(token)
        val anime = checkAnime(url)
        val episode = if(episodeNumber != null) checkEpisode(anime, episodeNumber) else null

        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)
        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()
            if (existFavorite.status == status && existFavorite.episode != null && existFavorite.episode?.number == episodeNumber) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                existFavorite.status = status
                if(episode != null && existFavorite.episode != episode)
                    existFavorite.episode = episode

                userFavoriteAnimeRepository.save(existFavorite)
                response.status = HttpStatus.OK.value()
                return
            }
        }

        userFavoriteAnimeRepository.save(UserFavoriteAnime(user = user, anime = anime, status = status, episode = episode))
        response.status = HttpStatus.CREATED.value()
    }

    override fun addToFavoritesManga(
        token: String,
        id: String,
        status: StatusFavourite,
        response: HttpServletResponse
    ) {
        val user = checkUser(token)
        val manga = checkManga(id)

        val existingFavorite = userFavoriteMangaRepository.findByUserAndManga(user, manga)
        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()
            if (existFavorite.status == status) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                existFavorite.status = status
                userFavoriteMangaRepository.save(existFavorite)
                response.status = HttpStatus.OK.value()
                return
            }
        }

        userFavoriteMangaRepository.save(UserFavouriteManga(user = user, manga = manga, status = status))
        response.status = HttpStatus.CREATED.value()
    }


    override fun addToRecentlyAnime(token: String, url: String, recently: RecentlyRequest, response: HttpServletResponse) {
        val user = checkUser(token)
        val anime = checkAnime(url)
        val episode = checkEpisode(anime, recently.episodeNumber)

        val existingRecently = userRecentlyRepository.findByUserAndAnime(user, anime)

        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)

        if (!existingFavorite.isPresent) {
            userFavoriteAnimeRepository.save(UserFavoriteAnime(user = user, anime = anime, status = StatusFavourite.Watching, episode = episode))
        }

        if (existingRecently.isPresent) {
            val existRecently = existingRecently.get()
            if (existRecently.date == recently.date && recently.timingInSeconds == existRecently.timingInSeconds && existRecently.episode == episode) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                existRecently.date = recently.date
                existRecently.timingInSeconds = recently.timingInSeconds
                existRecently.episode = episode

                userRecentlyRepository.save(existRecently)
                response.status = HttpStatus.OK.value()
                return
            }
        }

        userRecentlyRepository.save(UserRecentlyAnime(user = user, anime = anime, timingInSeconds = recently.timingInSeconds, date = recently.date, episode = episode))
        response.status = HttpStatus.CREATED.value()
    }


    override fun getRecentlyAnimeList(token: String, pageNum: Int, pageSize: Int): List<RecentlyAnimeLight> {
        val user = checkUser(token)

        return recentlyTableToRecentlyAnimeLight(userRecentlyRepository.findByUser(user))
    }

    override fun whoAmi(token: String): WhoAmi {
        val user = checkUser(token)

        return WhoAmi(
            username = user.username,
            email = user.email,
            nickName = user.nickName,
            typeUser = user.typeUser,
            roles = user.roles,
            image = user.image
        )
    }

    override fun getFavoritesAnimeByStatus(
        token: String,
        status: StatusFavourite,
        pageNum: Int,
        pageSize: Int
    ): List<AnimeLight> {
        val user = checkUser(token)

        return listToAnimeLight(
            userFavoriteAnimeRepository.findByUserAndStatus(
                user,
                status,
                PageRequest.of(pageNum, pageSize)
            ).map { it.anime })
    }

    override fun getFavoritesMangaByStatus(
        token: String,
        status: StatusFavourite,
        pageNum: Int,
        pageSize: Int
    ): List<MangaLight> {
        val user = checkUser(token)

        return listToMangaLight(
            userFavoriteMangaRepository.findByUserAndStatus(
                user,
                status,
                PageRequest.of(pageNum, pageSize)
            ).map { it.manga })
    }

    override fun setAnimeRating(token: String, url: String, rating: Int, response: HttpServletResponse) {
        val user = checkUser(token)
        val anime = checkAnime(url)

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
                if (prevRatingCount.count == 0)
                    userRatingCountRepository.deleteById(prevRatingCount.id)
                else
                    userRatingCountRepository.save(prevRatingCount)

                existRating.rating = rating
                userRatingRepository.save(existRating)
                if (existingRatingCount.isPresent) {
                    val existCount = existingRatingCount.get()
                    val a = existCount.count + 1
                    existCount.count = a
                    userRatingCountRepository.save(existCount)
                } else {
                    userRatingCountRepository.save(AnimeRatingCount(anime = anime, rating = rating, count = 1))
                }
                response.status = HttpStatus.OK.value()
            }
        } else {
            userRatingRepository.save(AnimeRating(anime = anime, rating = rating, user = user))
            if (existingRatingCount.isPresent) {
                val existCount = existingRatingCount.get()
                val a = existCount.count + 1
                existCount.count = a
                userRatingCountRepository.save(existCount)
            } else {
                userRatingCountRepository.save(AnimeRatingCount(anime = anime, rating = rating, count = 1))
            }
            response.status = HttpStatus.CREATED.value()
        }

        val animeRating = userRatingRepository.findByAnime(anime)

        val totalRating = animeRating.map { it.rating }.average()
        anime.totalRating = totalRating
        animeRepository.save(anime)
    }


    override fun setMangaRating(token: String, id: String, rating: Int, response: HttpServletResponse) {
        val user = checkUser(token)
        val manga = checkManga(id)

        val existingRatingCount = userRatingCountMangaRepository.findByMangaAndRating(manga, rating)

        val existingRating = userMangaRatingRepository.findByUserAndManga(user, manga)
        if (existingRating.isPresent) {
            val existRating = existingRating.get()
            if (existRating.rating == rating) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                val prevRatingCount = userRatingCountMangaRepository.findByMangaAndRating(manga, existRating.rating).get()
                prevRatingCount.count--
                if (prevRatingCount.count == 0)
                    userRatingCountMangaRepository.deleteById(prevRatingCount.id)
                else
                    userRatingCountMangaRepository.save(prevRatingCount)

                existRating.rating = rating
                userMangaRatingRepository.save(existRating)
                if (existingRatingCount.isPresent) {
                    val existCount = existingRatingCount.get()
                    val a = existCount.count + 1
                    existCount.count = a
                    userRatingCountMangaRepository.save(existCount)
                } else {
                    userRatingCountMangaRepository.save(MangaRatingCount(manga = manga, rating = rating, count = 1))
                }
                response.status = HttpStatus.OK.value()
            }
        } else {
            userMangaRatingRepository.save(MangaRating(manga = manga, rating = rating, user = user))
            if (existingRatingCount.isPresent) {
                val existCount = existingRatingCount.get()
                val a = existCount.count + 1
                existCount.count = a
                userRatingCountMangaRepository.save(existCount)
            } else {
                userRatingCountMangaRepository.save(MangaRatingCount(manga = manga, rating = rating, count = 1))
            }
            response.status = HttpStatus.CREATED.value()
        }

        val mangaRating = userMangaRatingRepository.findByManga(manga)

        val totalRating = mangaRating.map { it.rating }.average()
        manga.totalRating = totalRating
        mangaRepository.save(manga)
    }

    fun checkUser(token: String): User {
        return userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
    }

    fun checkAnime(url: String): AnimeTable {
        return animeRepository.findByUrl(url)
            .orElseThrow { NotFoundException("Anime not found") }
    }

    fun checkEpisode(anime: AnimeTable, episodeNumber: Int): AnimeEpisodeTable {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeEpisodeTable> = criteriaBuilder.createQuery(AnimeEpisodeTable::class.java)

        val animeRoot: Root<AnimeTable> = criteriaQuery.from(AnimeTable::class.java)

        val episodesJoin = animeRoot.join<AnimeTable, AnimeEpisodeTable>("episodes", JoinType.LEFT)

        criteriaQuery.where(criteriaBuilder.equal(animeRoot.get<String>("url"), anime.url))

        criteriaQuery.select(episodesJoin)

        criteriaQuery.where(criteriaBuilder.equal(episodesJoin.get<Int>("number"), episodeNumber))

        val query = entityManager.createQuery(criteriaQuery)

        val resultList = query.resultList

        if (resultList.isEmpty()) {
            throw NotFoundException("Episode not found")
        } else {
            return resultList[0]
        }
    }

    fun checkManga(id: String): MangaTable {
        return mangaRepository.findById(id)
            .orElseThrow { throw NotFoundException("Manga not found") }
    }

    fun recentlyTableToRecentlyAnimeLight(recently: List<UserRecentlyAnime>): List<RecentlyAnimeLight> {
        val recentlyReady = mutableListOf<RecentlyAnimeLight>()

        recently.forEach { recentlyItem ->
            recentlyReady.add(
                RecentlyAnimeLight(
                    anime = animeTableToAnimeLight(recentlyItem.anime),
                    date = recentlyItem.date,
                    timingInSeconds = recentlyItem.timingInSeconds,
                    episode = episodeToEpisodeLight(listOf(recentlyItem.episode))[0]
                )
            )
        }

        return recentlyReady
    }

}