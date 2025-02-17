package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeRatingDistributionTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeRatingDistributionRepository : JpaRepository<AnimeRatingDistributionTable, String>
