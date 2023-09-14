package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeImagesTypes
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.jpa.entity.anime.AnimeTable

fun AnimeTable.toAnimeLight() = with(this) {
    AnimeLight(
        url = url,
        title = title,
        image = AnimeImagesTypes(large = images.large, medium = images.medium),
        studio = studios.map { it.toStudio() },
        season = season,
        genres = genres.map { it.toGenre() },
        episodesCount = episodesCount,
        status = status,
        ratingMpa = ratingMpa,
        minimalAge = minimalAge,
        accentColor = accentColor,
        year = year,
        type = type,
        rating = totalRating,
    )
}
