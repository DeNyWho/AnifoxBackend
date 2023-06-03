package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanImages(
    @SerialName("jpg")
    val jikanJpg: JikanJpg = JikanJpg(),
)