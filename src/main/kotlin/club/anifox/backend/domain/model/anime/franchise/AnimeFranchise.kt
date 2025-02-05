package club.anifox.backend.domain.model.anime.franchise

import club.anifox.backend.domain.model.anime.light.AnimeLight
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeFranchise(
    val anime: AnimeLight,
    val relation: String,
    @SerialName("target_url")
    val targetUrl: String,
)
