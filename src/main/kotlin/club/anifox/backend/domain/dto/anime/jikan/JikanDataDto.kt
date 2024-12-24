package club.anifox.backend.domain.dto.anime.jikan

import club.anifox.backend.domain.dto.anime.jikan.image.JikanImagesDto
import club.anifox.backend.domain.dto.anime.jikan.image.JikanJpgDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanDataDto(
    @SerialName("mal_id")
    val malId: Int = 0,
    @SerialName("images")
    val images: JikanImagesDto<JikanJpgDto>,
    @SerialName("trailer")
    val trailer: JikanTrailerDto? = null,
)
