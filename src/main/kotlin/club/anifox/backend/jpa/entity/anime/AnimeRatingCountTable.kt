package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "rating_count", schema = "anime")
data class AnimeRatingCountTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),
    val rating: Int = 0,
    var count: Int = 0,
)
