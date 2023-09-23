package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanImagesDto(
    @SerialName("jpg")
    val jikanJpg: JikanJpgDto = JikanJpgDto(),
)
