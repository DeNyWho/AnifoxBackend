package club.anifox.backend.domain.dto.anime.kitsu.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuEpisodeThumbnail(
    @SerialName("original")
    val original: String? = null,
    @SerialName("large")
    val large: String? = null,
)
