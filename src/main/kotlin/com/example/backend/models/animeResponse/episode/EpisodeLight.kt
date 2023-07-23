package com.example.backend.models.animeResponse.episode

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable

@Serializable
@JsonInclude(JsonInclude.Include.ALWAYS)
data class EpisodeLight(
    val title: String? = "",
    val description: String? = "",
    val number: Int,
    val image: String? = "",
    val translations: List<EpisodeTranslations> = listOf()
)