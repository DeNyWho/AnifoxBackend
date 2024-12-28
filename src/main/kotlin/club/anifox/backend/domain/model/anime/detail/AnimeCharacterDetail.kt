package club.anifox.backend.domain.model.anime.detail

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeCharacterDetail(
    val role: String,
    val image: String,
    val name: String,
    @SerialName("name_eng")
    val nameEn: String,
    @SerialName("name_kanji")
    val nameKanji: String,
    val about: String,
    val pictures: List<String>,
)
