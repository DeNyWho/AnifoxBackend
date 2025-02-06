package club.anifox.backend.service.anime.components.schedule

import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeScheduleTable
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class AnimeScheduleComponent(
    private val entityManager: EntityManager,
) {
    @Transactional
    fun updateSchedule(animeId: String, nextEpisodeDate: LocalDateTime?) {
        val anime = findAnime(animeId)
        val schedule = findSchedule(animeId)

        when {
            schedule == null && nextEpisodeDate != null -> {
                createSchedule(anime, nextEpisodeDate)
            }
            schedule != null -> {
                updateExistingSchedule(anime, schedule, nextEpisodeDate)
            }
        }
    }

    private fun findAnime(animeId: String): AnimeTable {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(AnimeTable::class.java)
        val root = query.from(AnimeTable::class.java)

        query.where(cb.equal(root.get<String>("id"), animeId))
        return entityManager.createQuery(query).singleResult
    }

    private fun findSchedule(animeId: String): AnimeEpisodeScheduleTable? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(AnimeEpisodeScheduleTable::class.java)
        val root = query.from(AnimeEpisodeScheduleTable::class.java)
        val anime = root.get<AnimeTable>("anime")

        query.where(cb.equal(anime.get<String>("id"), animeId))
        return entityManager.createQuery(query).resultList.firstOrNull()
    }

    @Transactional
    fun createSchedule(anime: AnimeTable, nextEpisodeDate: LocalDateTime) {
        val schedule = AnimeEpisodeScheduleTable(
            anime = anime,
            nextEpisodeDate = nextEpisodeDate,
            previousEpisodeDate = LocalDateTime.now(),
            dayOfWeek = nextEpisodeDate.dayOfWeek,
        )

        anime.nextEpisode = nextEpisodeDate
        entityManager.persist(schedule)
        entityManager.merge(anime)
    }

    @Transactional
    fun updateExistingSchedule(
        anime: AnimeTable,
        currentSchedule: AnimeEpisodeScheduleTable,
        nextEpisodeDate: LocalDateTime?,
    ) {
        when {
            shouldClearSchedule(nextEpisodeDate, currentSchedule) -> {
                entityManager.remove(currentSchedule)
                anime.nextEpisode = null
                entityManager.merge(anime)
            }
            shouldUpdateSchedule(nextEpisodeDate, currentSchedule) -> {
                val newSchedule = AnimeEpisodeScheduleTable(
                    anime = anime,
                    nextEpisodeDate = nextEpisodeDate,
                    previousEpisodeDate = currentSchedule.nextEpisodeDate
                        ?: currentSchedule.previousEpisodeDate,
                    dayOfWeek = nextEpisodeDate!!.dayOfWeek,
                )
                entityManager.remove(currentSchedule)
                entityManager.persist(newSchedule)
                anime.nextEpisode = nextEpisodeDate
                entityManager.merge(anime)
            }
        }
    }

    private fun shouldClearSchedule(
        nextEpisodeDate: LocalDateTime?,
        schedule: AnimeEpisodeScheduleTable,
    ): Boolean = nextEpisodeDate == null && (
        LocalDateTime.now().isAfter(schedule.previousEpisodeDate) ||
            schedule.previousEpisodeDate == null
        )

    private fun shouldUpdateSchedule(
        nextEpisodeDate: LocalDateTime?,
        schedule: AnimeEpisodeScheduleTable,
    ): Boolean = nextEpisodeDate != null && schedule.nextEpisodeDate != nextEpisodeDate
}
