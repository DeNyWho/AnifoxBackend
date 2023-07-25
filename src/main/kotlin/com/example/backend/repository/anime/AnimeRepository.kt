package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnimeRepository : JpaRepository<AnimeTable, String> {


    fun findByTitle(title: String): Optional<AnimeTable>

    fun findByShikimoriId(shikimoriID: Int): Optional<AnimeTable>


    @Query("Select distinct a.year from AnimeTable a order by a.year desc")
    fun findDistinctByYear(): List<String>

    fun findByUrl(@Param("url") url: String): Optional<AnimeTable>


    @Query("SELECT a FROM AnimeTable a LEFT JOIN FETCH a.rating where a.url = :url")
    fun findByUrlWithRating(@Param("url") url: String): Optional<AnimeTable>

    @Query("SELECT a FROM AnimeTable a LEFT JOIN FETCH a.otherTitles o WHERE a.url = :url")
    fun findDetails(@Param("url") url: String): Optional<AnimeTable>


}