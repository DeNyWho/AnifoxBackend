package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanTrailerDto(
    @SerialName("youtube_id")
    val youtubeID: String? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("embed_url")
    val embedUrl: String? = null,
    @SerialName("images")
    val images: JikanTrailerImagesDto? = null,
)
