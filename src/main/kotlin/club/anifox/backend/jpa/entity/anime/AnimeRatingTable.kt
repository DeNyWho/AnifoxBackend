package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.jpa.entity.user.UserTable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "anime_rating", schema = "anime")
data class AnimeRatingTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserTable = UserTable(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    var rating: Int = 0,
)
