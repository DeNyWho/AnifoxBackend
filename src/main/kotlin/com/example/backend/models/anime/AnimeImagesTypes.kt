package com.example.backend.models.anime

import kotlinx.serialization.Serializable

@Serializable
data class AnimeImagesTypes(
    val large: String = "",
    val medium: String = "",
    val cover: String? = null
)