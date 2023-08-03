package com.example.backend.models.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanPagination(
    @SerialName("last_visible_page")
    val lastVisiblePage: Int = 0,
    @SerialName("has_next_page")
    val hasNextPage: Boolean = true
)