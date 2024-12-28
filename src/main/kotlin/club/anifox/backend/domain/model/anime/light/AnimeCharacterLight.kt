package club.anifox.backend.domain.model.anime.light

import kotlinx.serialization.Serializable

@Serializable
data class AnimeCharacterLight(
    val id: String,
    val role: String,
    val image: String,
    val name: String,
)
