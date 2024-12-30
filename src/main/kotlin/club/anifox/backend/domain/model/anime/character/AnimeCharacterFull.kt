package club.anifox.backend.domain.model.anime.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeCharacterFull(
    val id: String,
    val name: String,
    @SerialName("name_en")
    val nameEn: String,
    @SerialName("name_kanji")
    val nameKanji: String? = null,
    val image: String,
    @SerialName("about")
    val aboutRu: String?,
    val pictures: List<String>,
    val roles: List<AnimeCharacterRole>,
)
