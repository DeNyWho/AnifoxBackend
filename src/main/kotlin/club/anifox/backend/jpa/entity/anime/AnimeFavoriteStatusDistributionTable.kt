package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

@Entity
@Table(name = "anime_favorite_status_distribution", schema = "anime")
class AnimeFavoriteStatusDistributionTable(
    @OneToOne
    @MapsId
    @JoinColumn(name = "anime_id", nullable = false)
    val anime: AnimeTable,

    @Column(nullable = false)
    var watching: Int = 0,

    @Column(nullable = false)
    var completed: Int = 0,

    @Column(nullable = false, name = "on_hold")
    var onHold: Int = 0,

    @Column(nullable = false)
    var dropped: Int = 0,

    @Column(nullable = false, name = "plan_to_watch")
    var planToWatch: Int = 0,

    @Column(nullable = false)
    var total: Int = 0,

    @Column(nullable = false, name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    val id: String = ""

    @Version
    var version: Long = 0
}
