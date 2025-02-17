package club.anifox.backend.service.user.component.rating

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.anime.AnimeRatingDistributionTable
import club.anifox.backend.jpa.entity.anime.AnimeRatingTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.anime.AnimeRatingDistributionRepository
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.jpa.repository.user.anime.UserRatingRepository
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
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class AnimeRatingComponent(
    private val userUtils: UserUtils,
    private val animeUtils: AnimeUtils,
    private val userRatingRepository: UserRatingRepository,
    private val animeRepository: AnimeRepository,
    private val animeRatingDistributionRepository: AnimeRatingDistributionRepository,
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
    fun addRating(
        token: String,
        url: String,
        rating: Int,
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
                processAnimeRatingUpdate(user, anime, rating, response)
            } finally {
                lock.unlock()
            }
        } catch (e: Exception) {
            logger.error("Error processing rating update", e)
            throw e
        }
    }

    @Transactional
    fun deleteRating(
        token: String,
        url: String,
    ) {
        val user = userUtils.checkUser(token)
        val anime = animeUtils.checkAnime(url)
        val existingRating = userRatingRepository.findByUserAndAnime(user, anime)

        if (existingRating.isPresent) {
            val existRating = existingRating.get()
            updateRatingWithRetry(anime, existRating.rating, null)
            userRatingRepository.delete(existRating)
            userRatingRepository.flush()
        } else {
            throw NotFoundException("rating not found")
        }
    }

    @Transactional
    protected fun processAnimeRatingUpdate(
        user: UserTable,
        anime: AnimeTable,
        rating: Int,
        response: HttpServletResponse,
    ) {
        val existingRating = userRatingRepository.findByUserAndAnime(user, anime)

        if (existingRating.isPresent) {
            val existRating = existingRating.get()

            if (existRating.rating == rating) {
                return
            }

            updateRatingWithRetry(anime, existRating.rating, rating)

            existRating.apply {
                this.rating = rating
                this.updateDate = LocalDateTime.now()
            }

            userRatingRepository.saveAndFlush(existRating)
        } else {
            val newRating = AnimeRatingTable(
                user = user,
                anime = anime,
                rating = rating,
                updateDate = LocalDateTime.now(),
            )
            userRatingRepository.saveAndFlush(newRating)

            updateRatingWithRetry(anime, oldRating = null, newRating = rating)
            response.status = HttpStatus.CREATED.value()
        }
    }

    @Transactional
    protected fun updateRatingWithRetry(
        anime: AnimeTable,
        oldRating: Int?,
        newRating: Int?,
    ) {
        try {
            entityManager.find(
                AnimeRatingDistributionTable::class.java,
                anime.id,
                LockModeType.PESSIMISTIC_WRITE,
            )?.let { distribution ->
                updateDistribution(distribution, oldRating, newRating)
            } ?: createNewDistribution(anime, oldRating, newRating)

            updateTotalRating(anime)
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
        oldRating: Int?,
        newRating: Int?,
    ): AnimeRatingDistributionTable {
        val distribution = AnimeRatingDistributionTable(anime = anime, total = 1)
        entityManager.persist(distribution)
        entityManager.merge(anime)
        oldRating?.let { distribution.decrementScoreCount(it) }
        newRating?.let { distribution.incrementScoreCount(it) }
        return animeRatingDistributionRepository.saveAndFlush(distribution)
    }

    protected fun updateDistribution(
        distribution: AnimeRatingDistributionTable,
        oldRating: Int?,
        newRating: Int?,
    ) {
        oldRating?.let { distribution.decrementScoreCount(it) }
        newRating?.let { distribution.incrementScoreCount(it) }

        when {
            oldRating != null && newRating == null -> distribution.total--
            oldRating != null && newRating != null -> distribution.total
            else -> distribution.total++
        }

        animeRatingDistributionRepository.saveAndFlush(distribution)
    }

    @Transactional
    protected fun updateTotalRating(anime: AnimeTable) {
        val animeRatings = userRatingRepository.findByAnime(anime)
        val totalRating = animeRatings.map { it.rating }.average()
        anime.totalRating = totalRating
        animeRepository.save(anime)
    }
}
