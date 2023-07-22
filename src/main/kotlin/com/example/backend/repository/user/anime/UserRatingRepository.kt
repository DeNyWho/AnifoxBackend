package com.example.backend.repository.user.anime

import com.example.backend.jpa.anime.AnimeRating
import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface UserRatingRepository: JpaRepository<AnimeRating, String> {

    fun findByUserAndAnime(@Param("user") user: User, @Param("anime") anime: AnimeTable): Optional<AnimeRating>

    fun findByAnime(@Param("anime") anime: AnimeTable): List<AnimeRating>

}