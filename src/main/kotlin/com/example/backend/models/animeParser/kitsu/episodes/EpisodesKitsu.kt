package com.example.backend.models.animeParser.kitsu.episodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodesKitsu(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("attributes")
    val attributes: EpisodesAttributesKitsu? = null,
)