package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.AnimeSimilarTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeSimilarRepository : JpaRepository<AnimeSimilarTable, String> {
//    fun findAnimeSimilarTableByAnime(animeTable: AnimeTable): Optional<AnimeSimilarTable>
}
