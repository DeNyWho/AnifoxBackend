package com.example.backend.repository.user

import com.example.backend.jpa.manga.MangaTable
import com.example.backend.jpa.user.User
import com.example.backend.jpa.user.UserFavouriteManga
import com.example.backend.models.users.StatusFavourite
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface UserFavouriteMangaRepository: JpaRepository<UserFavouriteManga, String> {
    fun findByUserAndManga(@Param("user") user: User, @Param("manga") manga: MangaTable): Optional<UserFavouriteManga>
    fun findByUserAndStatus(@Param("user") user: User, @Param("status") status: StatusFavourite, pageable: Pageable): List<UserFavouriteManga>
}