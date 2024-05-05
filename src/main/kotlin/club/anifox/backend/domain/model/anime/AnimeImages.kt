package club.anifox.backend.domain.model.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeImages(
    val large: String = "",
    val medium: String = "",
    val cover: String? = null,
)
