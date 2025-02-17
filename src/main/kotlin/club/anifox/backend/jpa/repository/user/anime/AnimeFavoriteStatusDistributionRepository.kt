package club.anifox.backend.jpa.repository.user.anime

import club.anifox.backend.jpa.entity.anime.AnimeFavoriteStatusDistributionTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AnimeFavoriteStatusDistributionRepository : JpaRepository<AnimeFavoriteStatusDistributionTable, String> {
    fun findByAnime(anime: AnimeTable): AnimeFavoriteStatusDistributionTable?

    @Query("SELECT d FROM AnimeFavoriteStatusDistributionTable d WHERE d.anime = :anime")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByAnimeForUpdate(anime: AnimeTable): AnimeFavoriteStatusDistributionTable?
}
