package com.example.backend.models.animeParser.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.Nullable

@Serializable
data class AnimeMediaParse(
    @SerialName("status")
    val status: String = "",
    @SerialName("russian")
    val russian: String = "",
    @SerialName("license_name_ru")
    val russianLic: String? = null,
    @SerialName("description")
    val description: String = "",
    @Nullable
    @SerialName("english")
    val english: List<String?>,
    @Nullable
    @SerialName("synonyms")
    val synonyms: List<String?>,
    @Nullable
    @SerialName("japanese")
    val japanese: List<String?>,
    @SerialName("videos")
    val videos: List<AnimeVideoParse> = listOf(),
    @SerialName("fandubbers")
    val fandubbers: List<String> = listOf(),
    @SerialName("episodes")
    val episodes: Int = 0,
    @SerialName("episodes_aired")
    val episodesAired: Int = 0,
    @SerialName("aired_on")
    val airedAt: String? = null,
    @SerialName("released_on")
    val releasedAt: String? = null,
    @SerialName("next_episode_at")
    val nextEpisodeAt: String? = null
)