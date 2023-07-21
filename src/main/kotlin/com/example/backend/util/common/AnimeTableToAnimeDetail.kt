package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.models.anime.AnimeImagesTypes
import com.example.backend.models.animeResponse.detail.AnimeDetail


fun animeTableToAnimeDetail(
    anime: AnimeTable
): AnimeDetail {
    return AnimeDetail(
        url = anime.url,
        title = anime.title,
        image = AnimeImagesTypes(large = anime.images.large, medium = anime.images.medium, cover = anime.images.cover),
        studio = anime.studios.toList(),
        season = anime.season,
        description = anime.description,
        otherTitles = anime.otherTitles.distinct(),
        shikimoriRating = anime.shikimoriRating,
        nextEpisode = anime.nextEpisode,
        year = anime.year,
        releasedAt = anime.releasedAt,
        airedAt = anime.airedAt,
        type = anime.type,
        rating = anime.totalRating,
        episodesCount = anime.episodesCount,
        episodesCountAired = anime.episodesAires,
        linkPlayer = anime.link,
        genres = anime.genres.toList(),
        status = anime.status,
        ratingMpa = anime.ratingMpa,
        minimalAge = anime.minimalAge
    )
}