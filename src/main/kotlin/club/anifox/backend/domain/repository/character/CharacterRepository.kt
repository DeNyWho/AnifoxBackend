package club.anifox.backend.domain.repository.character

import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull

interface CharacterRepository {

    fun getCharacterFull(characterId: String): AnimeCharacterFull
}
