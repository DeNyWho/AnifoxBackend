package com.example.backend.repository.user.anime

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.user.User
import com.example.backend.jpa.user.UserRecentlyAnime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface UserRecentlyRepository : JpaRepository<UserRecentlyAnime, String> {

    fun findByUserAndAnime(@Param("user") user: User, @Param("anime") anime: AnimeTable): Optional<UserRecentlyAnime>

    fun findByUser(@Param("user") user: User): List<UserRecentlyAnime>

}