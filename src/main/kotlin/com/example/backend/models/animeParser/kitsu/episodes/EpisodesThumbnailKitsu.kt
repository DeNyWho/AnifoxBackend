package com.example.backend.models.animeParser.kitsu.episodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodesThumbnailKitsu(
    @SerialName("original")
    val original: String? = null,
    @SerialName("large")
    val large: String? = null,
)