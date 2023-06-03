package com.example.backend.models.animeResponse.common

import kotlinx.serialization.Serializable

@Serializable
data class RatingResponse(
    val rating: Int = 0,
    val count: Int = 0
)