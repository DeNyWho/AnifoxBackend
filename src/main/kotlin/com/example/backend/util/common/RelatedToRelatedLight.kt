package com.example.backend.util.common

import com.example.backend.jpa.anime.AnimeRelatedTable
import com.example.backend.models.animeResponse.light.RelatedLight

fun relatedToLight(
    related: AnimeRelatedTable
): RelatedLight = RelatedLight(
    typeEn = related.typeEn,
    type = related.type
)