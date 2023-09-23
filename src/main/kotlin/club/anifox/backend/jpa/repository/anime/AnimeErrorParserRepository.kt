package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeErrorParserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeErrorParserRepository : JpaRepository<AnimeErrorParserTable, String>
