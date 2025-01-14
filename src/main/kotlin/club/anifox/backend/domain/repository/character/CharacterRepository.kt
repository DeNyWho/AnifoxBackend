package club.anifox.backend.domain.repository.character

import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull
import club.anifox.backend.domain.model.anime.character.AnimeCharacterSitemap

interface CharacterRepository {

    fun getCharacterFull(characterId: String): AnimeCharacterFull
    fun getCharactersSitemap(): List<AnimeCharacterSitemap>
}
