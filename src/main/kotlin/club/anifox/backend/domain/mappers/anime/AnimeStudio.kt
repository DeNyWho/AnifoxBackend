package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.jpa.entity.anime.AnimeStudiosTable

fun AnimeStudiosTable.toStudio(): AnimeStudio {
    return AnimeStudio(
        id = id,
        studio = studio,
    )
}
