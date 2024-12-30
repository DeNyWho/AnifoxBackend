package club.anifox.backend.domain.dto.anime.jikan.character

import club.anifox.backend.domain.dto.anime.jikan.image.JikanImagesDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanCharacterDto(
    @SerialName("mal_id")
    val malId: Int,
    @SerialName("images")
    val images: JikanImagesDto<JikanJpgCharacterDto>,
    @SerialName("name")
    val name: String,
    @SerialName("name_kanji")
    val nameKanji: String?,
    @SerialName("about")
    val about: String?,
    @SerialName("anime")
    val anime: List<JikanCharacterAnimeRoleDto>,
)
