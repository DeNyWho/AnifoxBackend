package club.anifox.backend.domain.model.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeMedia(
    val url: String = "",
    val imageUrl: String = "",
    val playerUrl: String = "",
    val name: String = "",
    val kind: String = "",
    val hosting: String = "",
)
