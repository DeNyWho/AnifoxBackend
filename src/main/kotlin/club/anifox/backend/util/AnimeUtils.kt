package club.anifox.backend.util

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import org.springframework.stereotype.Component

@Component
class AnimeUtils(
    private val animeRepository: AnimeRepository,
    private val entityManager: EntityManager,
) {

    fun checkAnime(url: String): AnimeTable {
        return animeRepository.findByUrl(url).orElseThrow { NotFoundException("Anime not found") }
    }

    fun checkEpisode(url: String, episodeNumber: Int): AnimeEpisodeTable {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        criteriaQuery.select(root)
            .where(criteriaBuilder.equal(root.get<String>("url"), url))

        val anime: AnimeTable? = entityManager.createQuery(criteriaQuery).singleResult

        val episode = anime?.episodes?.find { it.number == episodeNumber }

        if (episode != null) return episode

        throw NotFoundException("Anime episode not found")
    }
}
