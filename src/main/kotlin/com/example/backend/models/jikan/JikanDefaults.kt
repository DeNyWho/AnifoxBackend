package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanDefaults<T>(
    @SerialName("data")
    val data: List<T> = listOf(),
    @SerialName("pagination")
    val pagination: JikanPagination,
)