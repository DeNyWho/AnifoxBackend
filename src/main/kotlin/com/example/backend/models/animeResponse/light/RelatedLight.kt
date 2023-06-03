package com.example.backend.models.animeResponse.light

import kotlinx.serialization.Serializable

@Serializable
data class RelatedLight(
    val type: String?,
    val typeEn: String?
)