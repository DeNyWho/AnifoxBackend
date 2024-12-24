package club.anifox.backend.domain.dto.anime.jikan.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanImagesDto<T>(
    @SerialName("jpg")
    val jikanJpg: T,
)
