package club.anifox.backend.domain.model.anime

import club.anifox.backend.domain.enums.anime.AnimeVideoType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeVideo(
    val url: String = "",
    @SerialName("image")
    val imageUrl: String = "",
    @SerialName("player_url")
    val playerUrl: String = "",
    val name: String = "",
    val type: AnimeVideoType,
)
