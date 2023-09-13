package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeStudiosTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeStudiosRepository : JpaRepository<AnimeStudiosTable, String> {
    @Query("Select s from AnimeStudiosTable s where :studio = s.studio")
    fun findByStudio(studio: String): Optional<AnimeStudiosTable>
}
