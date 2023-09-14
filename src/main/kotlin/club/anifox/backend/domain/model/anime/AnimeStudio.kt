package club.anifox.backend.domain.model.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeStudio(
    val id: String,
    val studio: String,
)
