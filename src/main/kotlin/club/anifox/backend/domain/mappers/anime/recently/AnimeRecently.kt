package club.anifox.backend.domain.mappers.anime.recently

import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.mappers.anime.toAnimeEpisodeLight
import club.anifox.backend.domain.model.anime.recently.AnimeRecently
import club.anifox.backend.jpa.entity.user.UserRecentlyAnimeTable

fun UserRecentlyAnimeTable.toRecently(): AnimeRecently {
    return AnimeRecently(
        anime = anime.toAnimeLight(),
        date = date,
        timingInSeconds = timingInSeconds,
        episode = episode.toAnimeEpisodeLight(),
        translationId = selectedTranslation.translation.id,
    )
}
