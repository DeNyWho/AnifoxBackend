package com.example.backend.models.animeResponse.episode

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeLight(
    val title: String? = "",
    val description: String? = "",
    val number: Int = 0,
    val image: String? = "",
    val link: String = "",
    val translations: List<EpisodeTranslations> = listOf()
)