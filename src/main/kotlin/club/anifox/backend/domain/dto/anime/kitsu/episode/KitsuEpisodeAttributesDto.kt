package club.anifox.backend.domain.dto.anime.kitsu.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuEpisodeAttributesDto(
    @SerialName("description")
    val description: String? = null,
    @SerialName("titles")
    val titles: KitsuEpisodeTitle = KitsuEpisodeTitle(),
    @SerialName("number")
    val number: Int? = null,
    @SerialName("relativeNumber")
    val relativeNumber: Int? = null,
    @SerialName("airdate")
    val airDate: String? = null,
    @SerialName("length")
    val length: Int? = null,
    @SerialName("thumbnail")
    val thumbnail: KitsuEpisodeThumbnail? = null,
)
