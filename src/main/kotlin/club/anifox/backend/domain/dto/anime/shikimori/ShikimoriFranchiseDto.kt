package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriFranchiseDto(
    @SerialName("links")
    val links: List<ShikimoriLinksDto>,
)
