package com.example.backend.jpa.user

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeTable
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*


@Entity
@Table(name = "user_recently_anime", schema = "users")
data class UserRecentlyAnime(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    var timingInSeconds: Double = 0.0,

    var date: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    var episode: AnimeEpisodeTable = AnimeEpisodeTable()
)
