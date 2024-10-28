package club.anifox.backend.jpa.entity.anime.episodes

import club.anifox.backend.jpa.entity.anime.AnimeTable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "anime_episode_schedule", schema = "anime")
data class AnimeEpisodeScheduleTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false, unique = true)
    val anime: AnimeTable,

    @Column(name = "next_episode_date", nullable = false)
    val nextEpisodeDate: LocalDateTime,

    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING)
    val dayOfWeek: DayOfWeek,
)
