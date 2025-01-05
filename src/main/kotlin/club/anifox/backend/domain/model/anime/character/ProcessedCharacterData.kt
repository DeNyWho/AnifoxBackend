package club.anifox.backend.domain.model.anime.character

import club.anifox.backend.jpa.entity.anime.AnimeCharacterRoleTable
import club.anifox.backend.jpa.entity.anime.AnimeCharacterTable

data class ProcessedCharacterData(
    val character: AnimeCharacterTable?,
    val roles: List<AnimeCharacterRoleTable>,
)
