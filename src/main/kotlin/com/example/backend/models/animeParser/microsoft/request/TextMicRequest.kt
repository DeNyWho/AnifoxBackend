package com.example.backend.models.animeParser.microsoft.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextMicRequest(
    @SerialName("Text")
    val text: String,
)