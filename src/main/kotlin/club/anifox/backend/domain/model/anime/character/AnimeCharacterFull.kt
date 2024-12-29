package club.anifox.backend.domain.model.anime.character

data class AnimeCharacterFull(
    val id: String,
    val name: String,
    val nameEn: String,
    val nameKanji: String,
    val image: String,
    val aboutRu: String?,
    val pictures: List<String>,
    val roles: List<AnimeCharacterRole>,
)
