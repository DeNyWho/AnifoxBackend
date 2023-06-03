package com.example.backend.jpa.manga

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.user.User
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min


@Entity
@Table(name = "manga_rating", schema = "manga")
data class MangaRating(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manga_id")
    val manga: MangaTable = MangaTable(),

    @field:Min(1)
    @field:Max(10)
    var rating: Int = 0
)