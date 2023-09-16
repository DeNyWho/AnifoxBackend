package club.anifox.backend.domain.dto.anime.kitsu.default

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuLinkDefaultDto(
    @SerialName("first")
    val first: String? = null,
    @SerialName("prev")
    val prev: String? = null,
    @SerialName("next")
    val next: String? = null,
    @SerialName("last")
    val last: String? = null,
)
