package com.example.backend.models.animeResponse.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeWithLink(
    @SerialName("playerLink")
    val link: String = "",
    @SerialName("episodes")
    val episodes: List<EpisodeLight> = listOf()
)
