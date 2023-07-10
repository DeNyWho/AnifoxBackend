package com.example.backend.models.animeParser.microsoft.default

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextTranslations(
    @SerialName("translations")
    val translations: List<TranslationMicrosoft> = listOf()
)