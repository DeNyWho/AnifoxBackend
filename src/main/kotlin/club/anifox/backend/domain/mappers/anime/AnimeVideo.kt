package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.jpa.entity.anime.AnimeVideoTable

fun AnimeVideoTable.toAnimeVideo() = with(this) {
    AnimeVideo(
        url = url,
        imageUrl = imageUrl,
        playerUrl = playerUrl,
        name = name,
        type = type,
    )
}
