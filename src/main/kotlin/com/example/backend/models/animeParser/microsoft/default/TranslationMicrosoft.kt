package com.example.backend.models.animeParser.microsoft.default

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationMicrosoft(
    @SerialName("text")
    val text: String? = null,
    @SerialName("to")
    val to: String? = null,
)