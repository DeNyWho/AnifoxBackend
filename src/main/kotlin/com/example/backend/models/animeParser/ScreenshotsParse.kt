package com.example.backend.models.animeParser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenshotsParse(
    @SerialName("original")
    val original: String = ""
)