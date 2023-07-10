package com.example.backend.models.animeParser.kitsu

import com.example.backend.models.animeParser.kitsu.default.LinksDefaultKitsu
import com.example.backend.models.animeParser.kitsu.default.MetaDefaultKitsu
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class KitsuDetails<T>(
    @SerialName("data")
    var data: T? = null
)