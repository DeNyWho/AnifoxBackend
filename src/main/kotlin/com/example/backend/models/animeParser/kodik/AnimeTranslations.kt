package com.example.backend.models.animeParser.kodik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeTranslations(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("count")
    val count: Int
)