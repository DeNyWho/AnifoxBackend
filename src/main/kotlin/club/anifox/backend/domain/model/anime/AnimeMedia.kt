package club.anifox.backend.domain.model.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeMedia(
    val url: String = "",
    @SerialName("image")
    val imageUrl: String = "",
    @SerialName("player_url")
    val playerUrl: String = "",
    val name: String = "",
    val kind: String = "",
    val hosting: String = "",
)
