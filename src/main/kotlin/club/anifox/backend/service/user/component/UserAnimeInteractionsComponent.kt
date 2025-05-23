package club.anifox.backend.service.user.component

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeProgressRequest
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import club.anifox.backend.jpa.entity.user.UserProgressAnimeTable
import club.anifox.backend.jpa.entity.user.UserRecentlyAnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.anime.UserFavoriteAnimeRepository
import club.anifox.backend.jpa.repository.user.anime.UserProgressAnimeRepository
import club.anifox.backend.jpa.repository.user.anime.UserRecentlyRepository
import club.anifox.backend.util.anime.AnimeUtils
import club.anifox.backend.util.user.UserUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserAnimeInteractionsComponent {
    @Autowired
    private lateinit var criteriaBuilder: CriteriaBuilder

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRecentlyRepository: UserRecentlyRepository

    @Autowired
    private lateinit var userFavoriteAnimeRepository: UserFavoriteAnimeRepository

    @Autowired
    private lateinit var userProgressAnimeRepository: UserProgressAnimeRepository

    @Autowired
    private lateinit var userUtils: UserUtils

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    fun getRecommendations(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        val user = userUtils.checkUser(token)

        val pageableFavorite = PageRequest.of(0, 10)

        val criteriaQueryFavorite = criteriaBuilder.createQuery(UserFavoriteAnimeTable::class.java)
        val favoriteRoot = criteriaQueryFavorite.from(UserFavoriteAnimeTable::class.java)

        val userJoin = favoriteRoot.join<UserFavoriteAnimeTable, UserTable>(UserFavoriteAnimeTable::user.name)
        val translationJoin = userJoin.join<AnimeTable, AnimeTranslationTable>("translations")

        criteriaQueryFavorite.where(criteriaBuilder.equal(translationJoin, user))

        val updateDateOrder = criteriaBuilder.desc(favoriteRoot.get<LocalDateTime>(UserFavoriteAnimeTable::updateDate.name))
        criteriaQueryFavorite.orderBy(updateDateOrder)

        val query = entityManager.createQuery(criteriaQueryFavorite)
        query.firstResult = pageableFavorite.pageNumber * pageableFavorite.pageSize
        query.maxResults = pageableFavorite.pageSize
        val favorite = query.resultList

        if (favorite.isNotEmpty()) {
            return recommendationProcess(favorite, PageRequest.of(page, limit))
        }

        throw NotFoundException("The user has not recommendations")
    }

    private fun recommendationProcess(
        favorite: List<UserFavoriteAnimeTable>,
        pageable: Pageable,
    ): List<AnimeLight> {
        val genres = favorite.flatMap { it.anime.genres }.distinct()
        val studios = favorite.flatMap { it.anime.studios }.distinct()
        val translations = favorite.flatMap { it.anime.translations }.distinct()

        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)
        criteriaQuery.select(root)

        val predicates: MutableList<Predicate> = mutableListOf()
        for (genre in genres) {
            val genrePredicate = criteriaBuilder.isMember(genre, root.get<List<AnimeGenreTable>>("genres"))
            predicates.add(genrePredicate)
        }

        for (studio in studios) {
            val studioPredicate = criteriaBuilder.isMember(studio, root.get<List<AnimeStudioTable>>("studios"))
            predicates.add(studioPredicate)
        }

        val translationIdsPredicate = criteriaBuilder.isMember(translations, root.get<List<AnimeTranslationTable>>("studios"))

        predicates.add(translationIdsPredicate)

        val query = entityManager.createQuery(criteriaQuery)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return query.resultList.map { it.toAnimeLight() }
    }

    fun updatePreferredGenres(
        token: String,
        genres: List<String>,
        response: HttpServletResponse,
    ) {
        val user = userUtils.checkUser(token)
        val genresExist = animeUtils.checkGenres(genres)

        if (genresExist.isEmpty()) {
            throw NotFoundException("Genres not found")
        }

        user.addPreferredGenres(genresExist)
    }

    fun getRecentlyAnimeList(
        token: String,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        val user = userUtils.checkUser(token)
        val recently = userRecentlyRepository.findByUser(user, PageRequest.of(page, limit))

        if (recently.isNotEmpty()) {
            return recently.map { it.anime.toAnimeLight() }
        }

        throw NotFoundException("The user has no recent anime")
    }

    fun changeEpisodeProgress(
        token: String,
        url: String,
        episodeNumber: Int,
        progress: AnimeEpisodeProgressRequest,
    ) {
        val anime = animeUtils.checkAnime(url)
        val user = userUtils.checkUser(token)
        val episode = animeUtils.checkEpisode(url, episodeNumber)
        val translation =
            episode.translations.find { it.translation.id == progress.translationId }
                ?: throw NotFoundException("Translation not found")

        val existingRecently = userRecentlyRepository.findByUserAndAnime(user, anime).orElse(null)
        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime).orElse(null)
        val existingProgress = userProgressAnimeRepository.findByUserAndAnimeAndEpisode(user, anime, episode).orElse(null)

        val isEpisodeWatched = anime.episodesCount == episode.number && progress.timingInSeconds > 1000

        val favoriteStatus =
            when {
                isEpisodeWatched -> StatusFavourite.Completed
                else -> StatusFavourite.Watching
            }

        val favorite =
            existingFavorite ?: UserFavoriteAnimeTable(
                user = user,
                anime = anime,
                episodesWatched = episode.number,
                status = favoriteStatus,
                updateDate = LocalDateTime.now(),
            )

        favorite.episodesWatched = episode.number
        favorite.status =
            when (favorite.status) {
                StatusFavourite.Watching -> if (isEpisodeWatched) StatusFavourite.Completed else StatusFavourite.Watching
                StatusFavourite.InPlan, StatusFavourite.Dropped, StatusFavourite.Completed, StatusFavourite.OnHold -> StatusFavourite.Watching
            }

        userFavoriteAnimeRepository.save(favorite)

        val progressRecord =
            existingProgress ?: UserProgressAnimeTable(
                user = user,
                anime = anime,
                episode = episode,
                selectedTranslation = translation,
                timing = progress.timingInSeconds,
            )

        progressRecord.timing = progress.timingInSeconds
        progressRecord.selectedTranslation = translation

        userProgressAnimeRepository.save(progressRecord)

        if (existingRecently == null) {
            userRecentlyRepository.save(
                UserRecentlyAnimeTable(user = user, anime = anime),
            )
        }
    }
}
