package com.example.backend.repository.user

import com.example.backend.jpa.anime.AnimeRatingCount
import com.example.backend.jpa.manga.MangaRatingCount
import com.example.backend.jpa.manga.MangaTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRatingCountMangaRepository : JpaRepository<MangaRatingCount, String> {

    fun findByMangaAndRating(@Param("manga") manga: MangaTable, @Param("rating") rating: Int): Optional<MangaRatingCount>
}