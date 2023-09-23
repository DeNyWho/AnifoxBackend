package club.anifox.backend.jpa.repository.user.anime

import club.anifox.backend.jpa.entity.anime.AnimeRatingTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRatingRepository : JpaRepository<AnimeRatingTable, String> {
    fun findByUserAndAnime(@Param("user") user: UserTable, @Param("anime") anime: AnimeTable): Optional<AnimeRatingTable>

    fun findByAnime(@Param("anime") anime: AnimeTable): List<AnimeRatingTable>
}
