package club.anifox.backend.jpa.entity.user

import club.anifox.backend.domain.enums.user.StatusFavourite
import club.anifox.backend.jpa.entity.anime.AnimeTable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "user_favorites_anime", schema = "users")
data class UserFavoriteAnimeTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserTable = UserTable(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),
    var updateDate: LocalDateTime = LocalDateTime.now(),
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: StatusFavourite = StatusFavourite.Watching,
    @Column(length = 5, nullable = true)
    var episodesWatched: Int? = null,
)
