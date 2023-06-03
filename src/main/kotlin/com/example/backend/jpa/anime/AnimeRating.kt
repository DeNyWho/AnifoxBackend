package com.example.backend.jpa.anime

import com.example.backend.jpa.user.User
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Entity
@Table(name = "anime_rating", schema = "anime")
data class AnimeRating(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    @field:Min(1)
    @field:Max(10)
    var rating: Int = 0
)