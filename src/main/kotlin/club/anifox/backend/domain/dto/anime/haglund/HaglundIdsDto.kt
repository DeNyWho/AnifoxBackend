package club.anifox.backend.domain.dto.anime.haglund

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HaglundIdsDto(
    @SerialName("anidb")
    val aniDb: Int? = null,
    @SerialName("anilist")
    val aniList: Int? = null,
    @SerialName("anime-planet")
    val animePlanet: String? = null,
    @SerialName("anisearch")
    val aniSearch: Int? = null,
    @SerialName("imdb")
    val imdb: String? = null,
    @SerialName("kitsu")
    val kitsu: Int? = null,
    @SerialName("livechart")
    val liveChart: Int? = null,
    @SerialName("notify-moe")
    val notifyMoe: String? = null,
    @SerialName("themoviedb")
    val theMovieDb: Int? = null,
    @SerialName("myanimelist")
    val myAnimeList: Int? = null,
)
