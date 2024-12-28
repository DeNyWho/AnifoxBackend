package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeCharacterRoleRepository : JpaRepository<AnimeCharacterRoleTable, Long> {
    fun findByAnimeIdAndCharacterId(animeId: String, characterId: String): AnimeCharacterRoleTable?
}
