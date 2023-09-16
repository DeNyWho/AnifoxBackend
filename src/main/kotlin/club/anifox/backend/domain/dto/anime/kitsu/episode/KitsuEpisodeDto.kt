package club.anifox.backend.domain.dto.anime.kitsu.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuEpisodeDto(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("attributes")
    val attributes: KitsuEpisodeAttributesDto? = null,
)
