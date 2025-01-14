package club.anifox.backend.service.character.components.common

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.character.toCharacterFull
import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull
import club.anifox.backend.domain.model.anime.character.AnimeCharacterSitemap
import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import org.springframework.stereotype.Component

@Component
class CharactersCommonComponent {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun getCharacterFull(characterId: String): AnimeCharacterFull {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(AnimeCharacterTable::class.java)
        val root = query.from(AnimeCharacterTable::class.java)

        val roles = root.join<AnimeCharacterTable, AnimeCharacterRoleTable>("characterRoles", JoinType.LEFT)

        val anime = roles.join<AnimeCharacterRoleTable, AnimeTable>("anime", JoinType.LEFT)

        query.select(root).distinct(true).where(cb.equal(root.get<String>("id"), characterId))

        val characterTable = entityManager.createQuery(query).resultList.firstOrNull() ?: throw NotFoundException("Character not found")

        return characterTable.toCharacterFull()
    }

    fun getCharactersSitemap(): List<AnimeCharacterSitemap> {
        return entityManager.createQuery(
            """
            SELECT new club.anifox.backend.domain.model.anime.character.AnimeCharacterSitemap(a.id) FROM AnimeCharacterTable a
            """.trimIndent(),
            AnimeCharacterSitemap::class.java,
        )
            .resultList
    }
}
