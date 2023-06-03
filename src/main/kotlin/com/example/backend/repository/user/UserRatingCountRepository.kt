package com.example.backend.repository.user

import com.example.backend.jpa.anime.AnimeRatingCount
import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.manga.MangaTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRatingCountRepository : JpaRepository<AnimeRatingCount, String> {

    fun findByAnimeAndRating(@Param("anime") anime: AnimeTable, @Param("rating") rating: Int): Optional<AnimeRatingCount>

    fun findByAnime(@Param("anime") anime: AnimeTable): List<AnimeRatingCount>

}