package club.anifox.backend.domain.dto.anime.kodik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodikSeasonDto(
    @SerialName("link")
    val link: String = "",
    @SerialName("episodes")
    val episodes: Map<String, KodikEpisodeDto>,
)
