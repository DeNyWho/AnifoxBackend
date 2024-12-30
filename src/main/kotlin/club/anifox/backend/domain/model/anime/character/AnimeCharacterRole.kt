package club.anifox.backend.domain.model.anime.character

import club.anifox.backend.domain.model.anime.light.AnimeLight
import kotlinx.serialization.Serializable

@Serializable
data class AnimeCharacterRole(
    val role: String,
    val anime: AnimeLight,
)
