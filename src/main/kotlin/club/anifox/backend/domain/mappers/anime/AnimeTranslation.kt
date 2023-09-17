package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.translation.AnimeTranslation
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable

fun AnimeTranslationTable.toAnimeTranslation(): AnimeTranslation {
    return AnimeTranslation(
        id = id,
        title = title,
        voice = voice,
    )
}
