package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeRepository : JpaRepository<AnimeTable, String> {
    fun findByShikimoriId(
        @Param("shikimoriId") shikimoriId: Int,
    ): Optional<AnimeTable>

    @Query("SELECT a FROM AnimeTable a WHERE a.shikimoriId IN :ids")
    fun findByShikimoriIds(@Param("ids") ids: List<Int>): List<AnimeTable>

    @Query("Select distinct a.year from AnimeTable a order by a.year desc")
    fun findDistinctByYear(): List<String>

    fun findByUrl(
        @Param("url") url: String,
    ): Optional<AnimeTable>
}
