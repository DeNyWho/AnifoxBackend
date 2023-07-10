package com.example.backend.models.animeParser.kitsu.episodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodesTitlesKitsu(
    @SerialName("en_jp")
    val enToJp: String? = null,
    @SerialName("en_us")
    val enToUs: String? = null,
    @SerialName("ja_jp")
    val original: String? = null,
)