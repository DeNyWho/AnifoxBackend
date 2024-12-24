package club.anifox.backend.domain.dto.anime.jikan.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanCharacterAnimeRoleDto(
    @SerialName("role")
    val role: String,
    @SerialName("anime")
    val anime: JikanCharacterAnimeDto,
)
