package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeStudiosRepository : JpaRepository<AnimeStudioTable, String> {
    @Query("Select s from AnimeStudioTable s where s.name = :studio")
    fun findByStudio(studio: String): Optional<AnimeStudioTable>
}
