package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanTrailerImagesDto(
    @SerialName("maximum_image_url")
    val maximumImageUrl: String? = null,
)
