package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "related", schema = "anime")
data class AnimeRelatedTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = true)
    val type: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_anime_id")
    val relatedAnime: AnimeTable,
)
