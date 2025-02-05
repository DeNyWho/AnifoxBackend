package club.anifox.backend.jpa.repository.anime

import club.anifox.backend.jpa.entity.anime.common.AnimeFranchiseTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AnimeFranchiseRepository : JpaRepository<AnimeFranchiseTable, String> {

    @Query("Select f from AnimeFranchiseTable f where f.url = :url")
    fun findByUrl(url: String): AnimeFranchiseTable?
}
