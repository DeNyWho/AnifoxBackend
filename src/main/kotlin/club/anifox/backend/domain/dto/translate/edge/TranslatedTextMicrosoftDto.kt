package club.anifox.backend.domain.dto.translate.edge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslatedTextMicrosoftDto(
    @SerialName("text")
    val text: String? = null,
    @SerialName("to")
    val to: String? = null,
)
