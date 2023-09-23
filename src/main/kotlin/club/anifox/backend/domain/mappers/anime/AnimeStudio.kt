package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.jpa.entity.anime.AnimeStudioTable

fun AnimeStudioTable.toStudio(): AnimeStudio {
    return AnimeStudio(
        id = id,
        name = name,
    )
}
