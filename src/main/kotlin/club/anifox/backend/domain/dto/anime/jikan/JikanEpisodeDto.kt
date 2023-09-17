package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanEpisodeDto(
    @SerialName("mal_id")
    val id: Int = 0,
    @SerialName("title")
    val title: String = "",
    @SerialName("aired")
    val aired: String = "",
    @SerialName("filler")
    val filler: Boolean = false,
    @SerialName("recap")
    val recap: Boolean = false,
)
