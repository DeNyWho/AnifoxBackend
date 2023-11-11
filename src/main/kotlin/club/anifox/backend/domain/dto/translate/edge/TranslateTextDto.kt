package club.anifox.backend.domain.dto.translate.edge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslateTextDto(
    @SerialName("Text")
    val text: String = "null",
)
