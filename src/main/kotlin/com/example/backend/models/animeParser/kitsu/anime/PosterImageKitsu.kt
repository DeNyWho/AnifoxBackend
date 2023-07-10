package com.example.backend.models.animeParser.kitsu.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PosterImageKitsu(
    @SerialName("original")
    val original: String? = "",
    @SerialName("large")
    val large: String? = ""
)