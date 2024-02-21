package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeTranslationCountRepository : JpaRepository<AnimeEpisodeTranslationCountTable, String>
