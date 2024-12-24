package club.anifox.backend.domain.dto.anime.jikan.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanJpgCharacterDto(
    @SerialName("image_url")
    val imageUrl: String,
)
