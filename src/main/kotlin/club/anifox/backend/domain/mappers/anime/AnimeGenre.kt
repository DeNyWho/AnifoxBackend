package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable

fun AnimeGenreTable.toGenre(): AnimeGenre {
    return AnimeGenre(
        id = id,
        name = name,
    )
}
