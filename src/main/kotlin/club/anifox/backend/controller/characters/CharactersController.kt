package club.anifox.backend.controller.characters

import club.anifox.backend.domain.model.anime.character.AnimeCharacterFull
import club.anifox.backend.service.character.CharactersService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin("*")
@Tag(name = "CharactersAPI", description = "All about characters")
@RequestMapping("/api/characters")
class CharactersController {
    @Autowired
    lateinit var charactersService: CharactersService

    @GetMapping("/{id}")
    @Operation(summary = "characters full")
    fun getAnimeCharactersFull(
        @PathVariable id: String,
    ): AnimeCharacterFull {
        return charactersService.getCharacterFull(id)
    }
}
