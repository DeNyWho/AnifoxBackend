package club.anifox.backend.domain.mappers.anime.detail

import club.anifox.backend.domain.mappers.anime.toAnimeTranslation
import club.anifox.backend.domain.mappers.anime.toGenre
import club.anifox.backend.domain.mappers.anime.toStudio
import club.anifox.backend.domain.model.anime.AnimeImages
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.jpa.entity.anime.AnimeTable

fun AnimeTable.toAnimeDetail(): AnimeDetail {
    return AnimeDetail(
        url = url,
        title = title,
        image = AnimeImages(large = images.large, medium = images.medium, cover = images.cover),
        studio = studios.toList().map { it.toStudio() },
        season = season,
        description = description,
        titleOther = titleOther,
        titleEnglish = titleEn.toList(),
        titleJapan = titleJapan.toList(),
        synonyms = synonyms,
        nextEpisode = nextEpisode,
        year = year,
        releasedOn = releasedOn,
        airedOn = airedOn,
        type = type,
        rating = totalRating,
        shikimoriRating = shikimoriRating,
        episodes = episodesCount,
        episodesAired = episodesAired,
        playerLink = playerLink,
        genres = genres.toList().map { it.toGenre() },
        status = status,
        ratingMpa = ratingMpa,
        minimalAge = minimalAge,
        translations = translations.toList().map { it.toAnimeTranslation() },
    )
}
