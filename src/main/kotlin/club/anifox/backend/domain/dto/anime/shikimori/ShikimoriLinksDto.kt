package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriLinksDto(
    val id: Long,
    @SerialName("source_id")
    val sourceId: Int,
    @SerialName("target_id")
    val targetId: Int,
    val source: Int,
    val target: Int,
    val weight: Int,
    val relation: String,
)
