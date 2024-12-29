package club.anifox.backend.domain.model.anime.character

import club.anifox.backend.domain.model.anime.light.AnimeLight

data class AnimeCharacterRole(
    val role: String,
    val anime: AnimeLight,
)
