package club.anifox.backend.service.character

import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull
import club.anifox.backend.domain.repository.character.CharacterRepository
import club.anifox.backend.service.character.components.common.CharactersCommonComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CharactersService : CharacterRepository {

    @Autowired
    private lateinit var charactersCommonComponent: CharactersCommonComponent

    override fun getCharacterFull(characterId: String): AnimeCharacterFull {
        return charactersCommonComponent.getCharacterFull(characterId)
    }
}
