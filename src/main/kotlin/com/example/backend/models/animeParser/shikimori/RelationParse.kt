package com.example.backend.models.animeParser.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RelationParse(
    @SerialName("relation")
    val relation: String? = "",
    @SerialName("relation_russian")
    val relationRussian: String? = "",
    val anime: AnimeParseShik?,
    val manga: MangaParseShik?
)