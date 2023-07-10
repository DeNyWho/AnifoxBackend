package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.models.anime.AnimeImagesTypes
import com.example.backend.models.animeResponse.light.AnimeLight

fun listToAnimeLight(
    anime: List<AnimeTable>
): List<AnimeLight> {
    val animeLight = mutableListOf<AnimeLight>()
    anime.forEach {
        animeLight.add(
            AnimeLight(
                url = it.url,
                title = it.title,
                image = AnimeImagesTypes(posterLarge = it.images.large, posterMedium = it.images.medium),
                studio = it.studios.toList(),
                season = it.season,
                genres = it.genres.toList(),
                episodesCount = it.episodesCount,
                status = it.status,
                ratingMpa = it.ratingMpa,
                minimalAge = it.minimalAge,
                accentColor = it.accentColor,
                year = it.year,
                type = it.type,
                rating = it.totalRating
            )
        )
    }
    return animeLight
}