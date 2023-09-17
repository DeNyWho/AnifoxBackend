package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.translation.AnimeEpisodeTranslations
import club.anifox.backend.jpa.entity.anime.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.EpisodeTranslationTable

fun AnimeEpisodeTable.toAnimeEpisodeLight(): AnimeEpisodeLight {
    return AnimeEpisodeLight(
        title = title,
        description = description,
        number = number,
        image = image,
        translations = translationsNormal(translations),
    )
}

fun translationsNormal(translations: MutableSet<EpisodeTranslationTable>): List<AnimeEpisodeTranslations> {
    val readyTranslations = mutableListOf<AnimeEpisodeTranslations>()
    translations.forEach { translation ->
        readyTranslations.add(
            AnimeEpisodeTranslations(
                id = translation.translation.id,
                link = translation.link,
                title = translation.translation.title,
                type = translation.translation.voice,
            ),
        )
    }
    return readyTranslations.sortedBy { it.id }
}
