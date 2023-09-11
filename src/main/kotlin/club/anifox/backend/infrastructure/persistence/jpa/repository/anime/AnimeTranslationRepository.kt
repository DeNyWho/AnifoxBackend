package club.anifox.backend.infrastructure.persistence.jpa.repository.anime

import club.anifox.backend.infrastructure.persistence.jpa.entity.anime.AnimeTranslationTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeTranslationRepository : JpaRepository<AnimeTranslationTable, Int>
