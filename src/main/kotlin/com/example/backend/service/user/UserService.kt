package com.example.backend.service.user

import com.example.backend.jpa.anime.AnimeRating
import com.example.backend.jpa.anime.AnimeRatingCount
import com.example.backend.jpa.manga.MangaRating
import com.example.backend.jpa.manga.MangaRatingCount
import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.jpa.user.UserFavouriteManga
import com.example.backend.models.animeResponse.light.AnimeLight
import com.example.backend.models.mangaResponse.light.MangaLight
import com.example.backend.models.users.StatusFavourite
import com.example.backend.models.users.WhoAmi
import com.example.backend.repository.anime.AnimeRepository
import com.example.backend.repository.manga.MangaRepository
import com.example.backend.repository.user.*
import com.example.backend.util.TokenHelper
import com.example.backend.util.common.listToAnimeLight
import com.example.backend.util.common.listToMangaLight
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.NotFoundException

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
        response: HttpServletResponse
    ) {
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
        val anime = animeRepository.findByUrl(url)
            .orElseThrow { throw NotFoundException("Anime not found") }

        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)
        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()
            if (existFavorite.status == status) {
                response.status = HttpStatus.OK.value()
                return
            } else {
                existFavorite.status = status
                userFavoriteAnimeRepository.save(existFavorite)
                response.status = HttpStatus.OK.value()
                return
            }
        }

        userFavoriteAnimeRepository.save(UserFavoriteAnime(user = user, anime = anime, status = status))
        response.status = HttpStatus.CREATED.value()
    }

    override fun addToFavoritesManga(
        token: String,
        id: String,
        status: StatusFavourite,
        response: HttpServletResponse
    ) {
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
        val manga = mangaRepository.findById(id)
            .orElseThrow { throw NotFoundException("Manga not found") }

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

    override fun whoAmi(token: String): WhoAmi {
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
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
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
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
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
        return listToMangaLight(
            userFavoriteMangaRepository.findByUserAndStatus(
                user,
                status,
                PageRequest.of(pageNum, pageSize)
            ).map { it.manga })
    }

    override fun setAnimeRating(token: String, url: String, rating: Int, response: HttpServletResponse) {
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }

        val anime = animeRepository.findByUrl(url)
            .orElseThrow { throw NotFoundException("Anime not found") }

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
        val user = userRepository.findByUsername(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }

        val manga = mangaRepository.findById(id)
            .orElseThrow { throw NotFoundException("Manga not found") }

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

}