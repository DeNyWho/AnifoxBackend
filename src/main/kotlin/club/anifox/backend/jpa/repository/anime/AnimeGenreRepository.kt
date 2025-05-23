package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeGenreRepository : JpaRepository<AnimeGenreTable, String> {
    @Query("Select g from AnimeGenreTable g where g.name = :genre")
    fun findByGenre(genre: String): Optional<AnimeGenreTable>
}
