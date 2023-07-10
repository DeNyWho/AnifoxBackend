package com.example.backend.models.animeParser.kitsu.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeAttributesKitsu(
    val posterImage: PosterImageKitsu = PosterImageKitsu(),
    val coverImage: CoverImageKitsu = CoverImageKitsu(),
)