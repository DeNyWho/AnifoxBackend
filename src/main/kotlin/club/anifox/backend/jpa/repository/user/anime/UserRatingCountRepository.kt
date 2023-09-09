package club.anifox.backend.jpa.repository.user.anime

import club.anifox.backend.jpa.entity.anime.AnimeRatingCountTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRatingCountRepository : JpaRepository<AnimeRatingCountTable, String> {
    fun findByAnimeAndRating(@Param("anime") anime: AnimeTable, @Param("rating") rating: Int): Optional<AnimeRatingCountTable>

    fun findByAnime(@Param("anime") anime: AnimeTable): List<AnimeRatingCountTable>
}
