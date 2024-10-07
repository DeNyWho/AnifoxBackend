package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.common.AnimeVideoTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeVideoRepository : JpaRepository<AnimeVideoTable, String>
