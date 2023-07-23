package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.jpa.anime.AnimeTranslationTable
import com.example.backend.jpa.anime.EpisodeTranslation
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
                number = episode.number,
                translations = translationsNormal(episode.translations)
            )
        )
    }
    return episodeLight
}

fun translationsNormal(translations: MutableSet<EpisodeTranslation>): List<EpisodeTranslations> {
    val readyTranslations = mutableListOf<EpisodeTranslations>()
    translations.forEach { translation ->
        readyTranslations.add(
            EpisodeTranslations(
                link = translation.link,
                title = translation.translation.title ,
                type = translation.translation.voice
            )
        )
    }
    return readyTranslations
}