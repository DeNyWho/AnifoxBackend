package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "anime_ids", schema = "anime")
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
