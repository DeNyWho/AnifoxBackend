package club.anifox.backend.infrastructure.persistence.jpa.repository.user.anime

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.infrastructure.persistence.jpa.entity.anime.AnimeTable
import club.anifox.backend.infrastructure.persistence.jpa.entity.user.UserFavoriteAnimeTable
import club.anifox.backend.infrastructure.persistence.jpa.entity.user.UserTable
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserFavoriteRepository : JpaRepository<UserFavoriteAnimeTable, String> {
    fun findByUserTableAndAnime(@Param("user") user: UserTable, @Param("anime") anime: AnimeTable): Optional<UserFavoriteAnimeTable>
    fun findByUserTableAndStatus(@Param("user") user: UserTable, @Param("status") status: StatusFavourite, pageable: Pageable): List<UserFavoriteAnimeTable>
}
