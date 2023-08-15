package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeRepository : JpaRepository<AnimeTable, String> {

    fun findByShikimoriId(@Param("shikimoriId") shikimoriId: Int): Optional<AnimeTable>

    @Query("Select a from AnimeTable a LEFT JOIN FETCH a.translations RIGHT JOIN FETCH a.translationsCountEpisodes JOIN FETCH a.episodes where a.status = :status")
    fun findByIdForEpisodesUpdate(status: String): List<AnimeTable>

    @Query("Select a from AnimeTable a LEFT JOIN FETCH a.translations RIGHT JOIN FETCH a.translationsCountEpisodes JOIN FETCH a.episodes where a.shikimoriId = :shikimoriID")
    fun findByIdForEpisodesUpdateWithShikimoriId(shikimoriID: Int): AnimeTable

    @Query("SELECT a from AnimeTable a LEFT JOIN FETCH a.episodes where a.shikimoriId = :shikimoriID")
    fun findByShikimoriIdWithEpisodes(shikimoriID: Int): Optional<AnimeTable>


    @Query("Select distinct a.year from AnimeTable a order by a.year desc")
    fun findDistinctByYear(): List<String>

    fun findByUrl(@Param("url") url: String): Optional<AnimeTable>

}