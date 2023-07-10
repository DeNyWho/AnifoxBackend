package com.example.backend.models.animeParser.kitsu.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoverImageKitsu(
    @SerialName("original")
    val coverOriginal: String? = "",
)