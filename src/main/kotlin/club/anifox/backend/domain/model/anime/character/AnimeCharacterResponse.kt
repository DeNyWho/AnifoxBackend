package club.anifox.backend.domain.model.anime.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeCharacterResponse(
    val characters: List<AnimeCharacterLight>,
    @SerialName("available_roles")
    val availableRoles: List<String>,
)
