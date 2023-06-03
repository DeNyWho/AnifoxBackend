package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanData(
    @SerialName("mal_id")
    val malId: Int = 0,
    @SerialName("images")
    val images: JikanImages = JikanImages()
)