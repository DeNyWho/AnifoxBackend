package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "similar", schema = "anime")
data class AnimeSimilarTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_anime_id")
    val similarAnime: AnimeTable,
) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeSimilarTable) return false
        return id == other.id
    }

    override fun toString(): String {
        return "AnimeSimilarTable(id='$id')"
    }
}
