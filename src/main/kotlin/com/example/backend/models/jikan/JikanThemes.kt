package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanThemes(
    @SerialName("openings")
    val openings: List<String?> = listOf(),
    @SerialName("endings")
    val endings: List<String?> = listOf(),
)