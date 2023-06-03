package com.example.backend.jpa.user

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.models.users.StatusFavourite
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "user_favorites_anime", schema = "users")
data class UserFavoriteAnime(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: StatusFavourite = StatusFavourite.Watching
)
