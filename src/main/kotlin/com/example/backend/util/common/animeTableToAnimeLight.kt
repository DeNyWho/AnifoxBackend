package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.models.anime.AnimeImagesTypes
import com.example.backend.models.animeResponse.light.AnimeLight


fun animeTableToAnimeLight(
    anime: AnimeTable
): AnimeLight {
    return AnimeLight(
        url = anime.url,
        title = anime.title,
        image = AnimeImagesTypes(large = anime.images.large, medium = anime.images.medium),
        studio = anime.studios.toList(),
        season = anime.season,
        genres = anime.genres.toList(),
        episodesCount = anime.episodesCount,
        status = anime.status,
        ratingMpa = anime.ratingMpa,
        minimalAge = anime.minimalAge,
        accentColor = anime.accentColor,
        year = anime.year,
        type = anime.type,
        rating = anime.totalRating
    )
}