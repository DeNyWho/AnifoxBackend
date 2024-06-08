package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriLinksDto(
    @SerialName("source_id")
    val sourceId: Int,
    @SerialName("target_id")
    val targetId: Int,
    @SerialName("relation")
    val relation: String,
)
