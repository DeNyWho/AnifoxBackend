package club.anifox.backend.domain.model.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeGenre(
    val id: String,
    val name: String,
)
