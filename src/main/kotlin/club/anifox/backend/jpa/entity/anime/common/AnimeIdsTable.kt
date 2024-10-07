package club.anifox.backend.jpa.entity.anime.common

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.util.*

@Entity
@Table(name = "ids", schema = "anime")
@BatchSize(size = 10)
data class AnimeIdsTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
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
)
