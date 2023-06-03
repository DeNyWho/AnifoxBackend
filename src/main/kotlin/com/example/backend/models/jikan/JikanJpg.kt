package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanJpg(
    @SerialName("large_image_url")
    val largeImageUrl: String = ""
)