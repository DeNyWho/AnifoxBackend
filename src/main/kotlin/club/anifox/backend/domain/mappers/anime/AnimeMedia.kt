package club.anifox.backend.domain.mappers.anime

import club.anifox.backend.domain.model.anime.AnimeMedia
import club.anifox.backend.jpa.entity.anime.AnimeMediaTable

fun AnimeMediaTable.toAnimeMedia() = with(this) {
    AnimeMedia(
        url = url,
        imageUrl = imageUrl,
        playerUrl = playerUrl,
        name = name,
        kind = kind,
        hosting = hosting,
    )
}
