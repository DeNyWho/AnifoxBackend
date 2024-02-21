package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeTranslationRepository : JpaRepository<AnimeTranslationTable, Int>
