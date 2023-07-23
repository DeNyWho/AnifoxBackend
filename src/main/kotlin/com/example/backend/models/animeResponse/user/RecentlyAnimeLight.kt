package com.example.backend.models.animeResponse.user

import com.example.backend.models.animeResponse.episode.EpisodeLight
import com.example.backend.models.animeResponse.light.AnimeLight
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.ALWAYS)
data class RecentlyAnimeLight(
    val anime: AnimeLight,
    val date: LocalDateTime,
    val timingInSeconds: Double,
    val episode: EpisodeLight?,
    val translationId: Int,
)