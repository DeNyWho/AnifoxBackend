package club.anifox.backend.jpa.repository.user.anime

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserFavoriteAnimeRepository : JpaRepository<UserFavoriteAnimeTable, String> {
    fun findByUserAndAnime(
        @Param("user") user: UserTable,
        @Param("anime") anime: AnimeTable,
    ): Optional<UserFavoriteAnimeTable>

    fun findByUser(
        @Param("user") user: UserTable,
        pageable: Pageable,
    ): List<UserFavoriteAnimeTable>

    fun findByUserAndStatus(
        @Param("user") user: UserTable,
        @Param("status") status: StatusFavourite,
        pageable: Pageable,
    ): List<UserFavoriteAnimeTable>
}
