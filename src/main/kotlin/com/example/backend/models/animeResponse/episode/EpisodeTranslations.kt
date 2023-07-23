package com.example.backend.models.animeResponse.episode

import kotlinx.serialization.Serializable


@Serializable
data class EpisodeTranslations(
    val id: Int,
    val link: String,
    val title: String,
    val type: String
)