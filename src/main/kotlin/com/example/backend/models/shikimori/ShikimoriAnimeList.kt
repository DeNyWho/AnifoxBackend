package com.example.backend.models.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriAnimeList(
    @SerialName("target_id")
    val targetId: Int,
    @SerialName("score")
    val score: Int,
    @SerialName("status")
    val status: String,
    @SerialName("rewatches")
    val rewatches: Int,
    @SerialName("episodes")
    val episodes: Int
)