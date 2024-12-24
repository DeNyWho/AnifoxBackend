package club.anifox.backend.domain.dto.anime.jikan.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanJpgDto(
    @SerialName("large_image_url")
    val largeImageUrl: String = "",
    @SerialName("image_url")
    val mediumImageUrl: String = "",
    @SerialName("small_image_url")
    val smallImageUrl: String = "",
)
