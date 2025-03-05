package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.jpa.entity.user.UserTable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "rating", schema = "anime")
data class AnimeRatingTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserTable? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable? = null,
    var rating: Int = 0,
    var updateDate: LocalDateTime = LocalDateTime.now(),
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeRatingTable) return false
        return id == other.id
    }
}
