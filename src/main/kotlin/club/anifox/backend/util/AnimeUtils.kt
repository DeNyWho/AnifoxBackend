package club.anifox.backend.util

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Component

@Component
class AnimeUtils(
    private val animeRepository: AnimeRepository,
    private val entityManager: EntityManager,
) {

    fun checkAnime(url: String): AnimeTable {
        return animeRepository.findByUrl(url).orElseThrow { NotFoundException("Anime not found") }
    }

    fun checkGenres(genres: List<String>): List<AnimeGenreTable> {
        val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery: CriteriaQuery<AnimeGenreTable> = criteriaBuilder.createQuery(AnimeGenreTable::class.java)
        val root: Root<AnimeGenreTable> = criteriaQuery.from(AnimeGenreTable::class.java)

        val predicates: List<Predicate> = genres.map { id ->
            criteriaBuilder.equal(root.get<String>("id"), id)
        }

        val finalPredicate: Predicate = criteriaBuilder.or(*predicates.toTypedArray())

        criteriaQuery.select(root)
            .where(finalPredicate)

        val query = entityManager.createQuery(criteriaQuery)

        return query.resultList
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
