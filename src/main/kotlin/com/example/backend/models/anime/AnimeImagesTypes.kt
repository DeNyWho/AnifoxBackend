package com.example.backend.models.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeImagesTypes(
    val posterLarge: String = "",
    val posterMedium: String = "",
    val coverLarge: String? = null
)