package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanDataDto(
    @SerialName("mal_id")
    val malId: Int = 0,
    @SerialName("images")
    val images: JikanImagesDto = JikanImagesDto(),
)
