package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeEpisodeTable
import com.example.backend.models.animeResponse.episode.EpisodeLight

fun episodeToEpisodeLight(
    episode: List<AnimeEpisodeTable>
): List<EpisodeLight> {
    val episodeLight = mutableListOf<EpisodeLight>()
    episode.forEach {
        episodeLight.add(
            EpisodeLight(
                title = it.title,
                description = it.description,
                image = it.image,
                number = it.number
            )
        )
    }
    return episodeLight
}