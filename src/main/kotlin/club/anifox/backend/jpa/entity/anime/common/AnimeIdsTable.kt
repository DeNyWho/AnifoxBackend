package club.anifox.backend.jpa.entity.anime.common

import club.anifox.backend.jpa.entity.anime.AnimeTable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize

@Entity
@Table(name = "ids", schema = "anime")
@BatchSize(size = 10)
data class AnimeIdsTable(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    @MapsId
    val anime: AnimeTable,
    val aniDb: Int? = null,
    val aniList: Int? = null,
    val animePlanet: String? = null,
    val aniSearch: Int? = null,
    val imdb: String? = null,
    val kitsu: Int? = null,
    val liveChart: Int? = null,
    val notifyMoe: String? = null,
    val thetvdb: Int? = null,
    val myAnimeList: Int? = null,
) {
    @Id
    val id: String = ""
}
