package club.anifox.backend.service.user.component.favourite

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.jpa.entity.anime.AnimeFavoriteStatusDistributionTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.anime.AnimeFavoriteStatusDistributionRepository
import club.anifox.backend.jpa.repository.user.anime.UserFavoriteAnimeRepository
import club.anifox.backend.util.anime.AnimeUtils
import club.anifox.backend.util.user.UserUtils
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.OptimisticLockException
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.dao.ConcurrencyFailureException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class AnimeFavoriteStatusComponent(
    private val statusRepo: AnimeFavoriteStatusDistributionRepository,
    private val userFavoriteAnimeRepository: UserFavoriteAnimeRepository,
    private val userUtils: UserUtils,
    private val animeUtils: AnimeUtils,
    private val entityManager: EntityManager,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val distributionLocks = mutableMapOf<String, ReentrantLock>()

    @Retryable(
        value = [OptimisticLockException::class, PessimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    @Transactional
    fun addToFavorites(
        token: String,
        url: String,
        status: StatusFavourite,
        episodesWatched: Int?,
        response: HttpServletResponse,
    ) {
        try {
            val user = userUtils.checkUser(token)
            val anime = animeUtils.checkAnime(url)

            val lock = distributionLocks.computeIfAbsent(anime.id) { ReentrantLock() }

            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw ConcurrencyFailureException("Unable to acquire lock for anime ${anime.id}")
            }

            try {
                processAnimeStatusUpdate(user, anime, status, episodesWatched, response)
            } finally {
                lock.unlock()
            }
        } catch (e: Exception) {
            logger.error("Error processing favorite status update", e)
            throw e
        }
    }

    @Transactional
    fun deleteFavorite(
        token: String,
        url: String,
    ) {
        val user = userUtils.checkUser(token)
        val anime = animeUtils.checkAnime(url)
        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)

        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()

            updateStatusWithRetry(anime, existFavorite.status, null)
            userFavoriteAnimeRepository.delete(existFavorite)
            userFavoriteAnimeRepository.flush()
        } else {
            throw NotFoundException("favourite not found")
        }
    }

    fun getFavoritesByStatus(
        token: String,
        status: StatusFavourite,
        page: Int,
        limit: Int,
    ): List<AnimeLight> {
        val user = userUtils.checkUser(token)
        val favoriteAnime = userFavoriteAnimeRepository.findByUserAndStatus(
            user,
            status,
            PageRequest.of(page, limit),
        )

        return favoriteAnime.takeIf { it.isNotEmpty() }
            ?.map { it.anime.toAnimeLight() }
            ?: throw NotFoundException("favourite not found")
    }

    @Transactional
    protected fun processAnimeStatusUpdate(
        user: UserTable,
        anime: AnimeTable,
        status: StatusFavourite,
        episodesWatched: Int?,
        response: HttpServletResponse,
    ) {
        val existingFavorite = userFavoriteAnimeRepository.findByUserAndAnime(user, anime)

        if (existingFavorite.isPresent) {
            val existFavorite = existingFavorite.get()

            if (existFavorite.status == status && existFavorite.episodesWatched == episodesWatched) {
                return
            }

            updateStatusWithRetry(anime, existFavorite.status, status)

            existFavorite.apply {
                this.status = status
                this.episodesWatched = episodesWatched ?: this.episodesWatched
                this.updateDate = LocalDateTime.now()
            }

            userFavoriteAnimeRepository.saveAndFlush(existFavorite)
        } else {
            val newFavorite = UserFavoriteAnimeTable(
                user = user,
                anime = anime,
                status = status,
                updateDate = LocalDateTime.now(),
                episodesWatched = episodesWatched,
            )
            userFavoriteAnimeRepository.saveAndFlush(newFavorite)

            updateStatusWithRetry(anime, oldStatus = null, newStatus = status)
            response.status = HttpStatus.CREATED.value()
        }
    }

    // Rest of the methods remain unchanged
    @Transactional
    protected fun updateStatusWithRetry(
        anime: AnimeTable,
        oldStatus: StatusFavourite?,
        newStatus: StatusFavourite?,
    ) {
        try {
            entityManager.find(
                AnimeFavoriteStatusDistributionTable::class.java,
                anime.id,
                LockModeType.PESSIMISTIC_WRITE,
            )?.let { distribution ->
                updateDistribution(distribution, oldStatus, newStatus)
            } ?: createNewDistribution(anime, oldStatus, newStatus)
        } catch (e: Exception) {
            when (e) {
                is PessimisticLockingFailureException -> {
                    logger.warn("Lock acquisition failed for anime ${anime.id}", e)
                    throw e
                }
                is OptimisticLockException -> {
                    logger.warn("Concurrent modification detected for anime ${anime.id}", e)
                    throw e
                }
                else -> {
                    logger.error("Unexpected error updating status distribution", e)
                    throw e
                }
            }
        }
    }

    @Transactional
    protected fun createNewDistribution(
        anime: AnimeTable,
        oldStatus: StatusFavourite?,
        newStatus: StatusFavourite?,
    ): AnimeFavoriteStatusDistributionTable {
        val distribution = AnimeFavoriteStatusDistributionTable(anime = anime, total = 1)
        entityManager.persist(distribution)
        entityManager.merge(anime)
        oldStatus?.let { decrementStatus(distribution, it) }
        newStatus?.let { incrementStatus(distribution, it) }
        return statusRepo.saveAndFlush(distribution)
    }

    protected fun updateDistribution(
        distribution: AnimeFavoriteStatusDistributionTable,
        oldStatus: StatusFavourite?,
        newStatus: StatusFavourite?,
    ) {
        oldStatus?.let { decrementStatus(distribution, it) }
        newStatus?.let { incrementStatus(distribution, it) }

        when {
            oldStatus != null && newStatus == null -> distribution.total--
            oldStatus != null && newStatus != null -> distribution.total
            else -> distribution.total++
        }

        statusRepo.saveAndFlush(distribution)
    }

    private fun incrementStatus(distribution: AnimeFavoriteStatusDistributionTable, status: StatusFavourite) {
        when (status) {
            StatusFavourite.Watching -> distribution.watching++
            StatusFavourite.Completed -> distribution.completed++
            StatusFavourite.OnHold -> distribution.onHold++
            StatusFavourite.Dropped -> distribution.dropped++
            StatusFavourite.InPlan -> distribution.planToWatch++
        }
    }

    private fun decrementStatus(distribution: AnimeFavoriteStatusDistributionTable, status: StatusFavourite) {
        when (status) {
            StatusFavourite.Watching -> distribution.watching--
            StatusFavourite.Completed -> distribution.completed--
            StatusFavourite.OnHold -> distribution.onHold--
            StatusFavourite.Dropped -> distribution.dropped--
            StatusFavourite.InPlan -> distribution.planToWatch--
        }
    }
}
