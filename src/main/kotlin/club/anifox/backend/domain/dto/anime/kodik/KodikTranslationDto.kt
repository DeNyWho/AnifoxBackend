package club.anifox.backend.domain.dto.anime.kodik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodikTranslationDto(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("title")
    val title: String = "",
    @SerialName("type")
    val voice: String = "",
)
