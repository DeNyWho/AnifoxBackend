package club.anifox.backend.domain.model.anime

import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import kotlinx.serialization.Serializable

@Serializable
data class AnimeGenre(
    val id: String,
    val genre: String,
)

fun AnimeGenreTable.toGenre(): AnimeGenre {
    return AnimeGenre(
        id = id,
        genre = genre,
    )
}
