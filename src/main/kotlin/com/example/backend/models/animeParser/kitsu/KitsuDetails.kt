package com.example.backend.models.animeParser.kitsu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class KitsuDetails<T>(
    @SerialName("data")
    var data: T? = null
)