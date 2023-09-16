package club.anifox.backend.domain.dto.translate.edge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslatedTextDto(
    @SerialName("translations")
    val translations: List<TranslatedTextMicrosoftDto> = listOf()
)
