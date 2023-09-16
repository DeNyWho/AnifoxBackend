package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanThemesDto(
    @SerialName("openings")
    val openings: List<String?> = listOf(),
    @SerialName("endings")
    val endings: List<String?> = listOf(),
)
