package club.anifox.backend.jpa.repository.user.anime

import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.user.UserProgressAnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserProgressAnimeRepository : JpaRepository<UserProgressAnimeTable, String> {
    fun findByUserAndAnime(@Param("user") user: UserTable, @Param("anime") anime: AnimeTable): List<UserProgressAnimeTable>

    fun findByUserAndAnimeAndEpisode(@Param("user") user: UserTable, @Param("anime") anime: AnimeTable, @Param("episode") episode: AnimeEpisodeTable): Optional<UserProgressAnimeTable>
}
