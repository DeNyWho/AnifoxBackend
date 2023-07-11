package com.example.backend.repository.user

import com.example.backend.jpa.manga.MangaRating
import com.example.backend.jpa.manga.MangaTable
import com.example.backend.jpa.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserMangaRatingRepository : JpaRepository<MangaRating, String> {

    fun findByUserAndManga(@Param("user") user: User, @Param("manga") manga: MangaTable): Optional<MangaRating>

    fun findByManga(@Param("manga") manga: MangaTable): List<MangaRating>
}