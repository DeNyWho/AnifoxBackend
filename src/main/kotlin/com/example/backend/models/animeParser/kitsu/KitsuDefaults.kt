package com.example.backend.models.animeParser.kitsu

import com.example.backend.models.animeParser.kitsu.default.LinksDefaultKitsu
import com.example.backend.models.animeParser.kitsu.default.MetaDefaultKitsu
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuDefaults<T>(
    @SerialName("data")
    var data: List<T>? = null,
    @SerialName("meta")
    val meta: MetaDefaultKitsu = MetaDefaultKitsu(),
    @SerialName("links")
    val links: LinksDefaultKitsu = LinksDefaultKitsu()
)