package club.anifox.backend.domain.dto.anime.kodik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodikEpisodeDto(
    @SerialName("link")
    val link: String,
    @SerialName("screenshots")
    val screenshots: List<String>,
)
