package com.example.backend.models.animeParser.kitsu.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeKitsu(
    @SerialName("attributes")
    val attributesKitsu: AnimeAttributesKitsu = AnimeAttributesKitsu(),
)