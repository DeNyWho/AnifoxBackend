package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriExternalLinksDto(
    @SerialName("kind")
    val kind: String,
    @SerialName("url")
    val url: String,
)
