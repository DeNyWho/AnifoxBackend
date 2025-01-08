package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeCharacterRepository : JpaRepository<AnimeCharacterTable, String> {
    fun findAllByMalIdIn(malIds: List<Int>): List<AnimeCharacterTable>
}
