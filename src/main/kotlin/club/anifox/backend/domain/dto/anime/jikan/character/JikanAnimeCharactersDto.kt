package club.anifox.backend.domain.dto.anime.jikan.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanAnimeCharactersDto(
    @SerialName("character")
    val character: JikanCharacterIDDto,
)
