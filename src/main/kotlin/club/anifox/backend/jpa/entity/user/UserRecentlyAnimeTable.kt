package club.anifox.backend.jpa.entity.user

import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.EpisodeTranslationTable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "user_recently_anime", schema = "users")
data class UserRecentlyAnimeTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val userTable: UserTable = UserTable(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    var timingInSeconds: Double = 0.0,

    var date: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_translation_id")
    var selectedTranslation: EpisodeTranslationTable = EpisodeTranslationTable(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    var episode: AnimeEpisodeTable = AnimeEpisodeTable(),
)
