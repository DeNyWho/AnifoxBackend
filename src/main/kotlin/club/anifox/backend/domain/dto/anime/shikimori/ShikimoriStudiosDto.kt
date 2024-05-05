package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriStudiosDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String = "",
)
