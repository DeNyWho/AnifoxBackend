package com.example.backend.models.animeParser.kitsu.default

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaDefaultKitsu(
    @SerialName("count")
    val count: Int? = null
)