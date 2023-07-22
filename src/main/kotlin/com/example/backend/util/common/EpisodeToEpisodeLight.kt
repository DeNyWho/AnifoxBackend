package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeTranslationTable
import com.example.backend.models.animeResponse.episode.EpisodeLight
import com.example.backend.models.animeResponse.episode.EpisodeTranslations

fun episodeToEpisodeLight(
    episodes: List<AnimeEpisodeTable>
): List<EpisodeLight> {
    val episodeLight = mutableListOf<EpisodeLight>()
    episodes.forEach { episode ->
        println("WW = $episode")
        if(episode.number == 0) println("CURSEd")
        episodeLight.add(
            EpisodeLight(
                title = episode.title,
                description = episode.description,
                image = episode.image,
                link = episode.link,
                number = episode.number,
                translations = translationsNormal(episode.translations)
            )
        )
    }
    return episodeLight
}

fun translationsNormal(translations: MutableSet<AnimeTranslationTable>): List<EpisodeTranslations> {
    val readyTranslations = mutableListOf<EpisodeTranslations>()
    translations.forEach { translation ->
        readyTranslations.add(
            EpisodeTranslations(
                title = translation.title,
                type = translation.voice
            )
        )
    }
    return readyTranslations
}