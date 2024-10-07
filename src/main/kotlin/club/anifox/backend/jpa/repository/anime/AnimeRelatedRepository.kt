package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeRelatedTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeRelatedRepository : JpaRepository<AnimeRelatedTable, String> {
    fun findByAnimeAndType(animeTable: AnimeTable, type: String): Optional<AnimeRelatedTable>
}
