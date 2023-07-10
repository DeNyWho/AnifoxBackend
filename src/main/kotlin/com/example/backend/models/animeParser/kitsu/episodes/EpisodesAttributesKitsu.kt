package com.example.backend.models.animeParser.kitsu.episodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodesAttributesKitsu(
    @SerialName("description")
    val description: String? = null,
    @SerialName("titles")
    val titles: EpisodesTitlesKitsu = EpisodesTitlesKitsu(),
    @SerialName("number")
    val number: Int? = null,
    @SerialName("relativeNumber")
    val relativeNumber: Int? = null,
    @SerialName("airdate")
    val airDate: String? = null,
    @SerialName("thumbnail")
    val thumbnail: EpisodesThumbnailKitsu? = null
)