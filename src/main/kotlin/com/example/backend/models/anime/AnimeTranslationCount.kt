package com.example.backend.models.anime

import com.example.backend.jpa.anime.AnimeTranslationTable

data class AnimeTranslationCount(
    val translation: AnimeTranslationTable = AnimeTranslationTable(),
    val countEpisodes: Int = 0
)