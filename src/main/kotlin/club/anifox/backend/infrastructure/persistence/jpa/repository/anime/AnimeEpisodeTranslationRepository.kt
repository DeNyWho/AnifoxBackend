package club.anifox.backend.infrastructure.persistence.jpa.repository.anime

import club.anifox.backend.infrastructure.persistence.jpa.entity.anime.EpisodeTranslationTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeEpisodeTranslationRepository : JpaRepository<EpisodeTranslationTable, String>
