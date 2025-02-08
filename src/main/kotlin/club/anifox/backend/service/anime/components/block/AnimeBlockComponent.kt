package club.anifox.backend.service.anime.components.block

import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.entity.anime.AnimeBlockedTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.repository.anime.AnimeBlockedRepository
import club.anifox.backend.service.image.ImageService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeBlockComponent {

    @Autowired
    private lateinit var animeBlockedRepository: AnimeBlockedRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var imageService: ImageService

    @Transactional
    fun blockAnime(
        url: String? = null,
        shikimoriId: Int? = null,
    ) {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)

        root.fetch<AnimeTable, Any>("episodes", JoinType.LEFT)

        val predicate =
            when {
                url?.isNotEmpty() == true -> criteriaBuilder.equal(root.get<String>("url"), url)
                shikimoriId != null -> criteriaBuilder.equal(root.get<Int>("shikimoriId"), shikimoriId)
                else -> return
            }
        criteriaQuery.where(predicate)

        val query = entityManager.createQuery(criteriaQuery)
        val anime = query.resultList

        entityManager.flush()

        if (anime.isNotEmpty()) {
            val animeEntity = anime[0]
            val shikimoriIdEntity = animeEntity.shikimoriId

            // Delete related anime relationships in both directions
            entityManager.createQuery(
                """
                DELETE FROM AnimeRelatedTable r
                WHERE r.anime.id = :animeId
                OR r.relatedAnime.id = :animeId
            """,
            )
                .setParameter("animeId", animeEntity.id)
                .executeUpdate()

            // Delete similar anime relationships in both directions
            entityManager.createQuery(
                """
                DELETE FROM AnimeSimilarTable s
                WHERE s.anime.id = :animeId
                OR s.similarAnime.id = :animeId
            """,
            )
                .setParameter("animeId", animeEntity.id)
                .executeUpdate()

            animeEntity.apply {
                episodes.clear()
                translationsCountEpisodes.clear()
                translations.clear()
                favorites.clear()
                rating.clear()
                videos.clear()
                genres.clear()
                studios.clear()
                titleEn.clear()
                titleJapan.clear()
                synonyms.clear()
                titleOther.clear()
                episodes.clear()
                screenshots.clear()
                similar.clear()
                related.clear()
                images = AnimeImagesTable()
            }

            entityManager.remove(animeEntity)
            entityManager.flush()

            if (animeEntity.url.isNotEmpty()) {
                CompressAnimeImageType.entries.forEach { imageType ->
                    imageService.deleteObjectsInFolder("images/anime/${imageType.path}/${animeEntity.url}/")
                }
            }

            val animeBlocked =
                AnimeBlockedTable(
                    shikimoriID = shikimoriIdEntity,
                )

            animeBlockedRepository.saveAndFlush(animeBlocked)
        } else {
            if (shikimoriId != null) {
                val animeBlocked =
                    AnimeBlockedTable(
                        shikimoriID = shikimoriId,
                    )

                animeBlockedRepository.saveAndFlush(animeBlocked)
            }
        }
    }
}
