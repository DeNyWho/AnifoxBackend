package club.anifox.backend.domain.dto.anime.kitsu.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuPosterImageDto(
    @SerialName("original")
    val original: String? = "",
    @SerialName("large")
    val large: String? = "",
)
