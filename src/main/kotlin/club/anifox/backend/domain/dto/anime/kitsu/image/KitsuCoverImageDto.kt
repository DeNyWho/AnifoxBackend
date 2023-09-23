package club.anifox.backend.domain.dto.anime.kitsu.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuCoverImageDto(
    @SerialName("original")
    val coverOriginal: String? = null,
    @SerialName("large")
    val coverLarge: String? = null,
)
