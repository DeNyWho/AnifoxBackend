package com.example.backend.repository.user

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.manga.MangaTable
import com.example.backend.jpa.user.User
import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.models.users.StatusFavourite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserFavoriteAnimeRepository: JpaRepository<UserFavoriteAnime, String> {
    fun findByUserAndAnime(@Param("user") user: User, @Param("anime") anime: AnimeTable): Optional<UserFavoriteAnime>
    fun findByUserAndStatus(@Param("user") user: User, @Param("status") status: StatusFavourite, pageable: Pageable): List<UserFavoriteAnime>
}