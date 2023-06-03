package com.example.backend.models.animeResponse.light

import com.example.backend.jpa.anime.AnimeRelatedTable
import kotlinx.serialization.Serializable

@Serializable
data class AnimeLightWithType(
    val anime: AnimeLight,
    val related: RelatedLight,
)