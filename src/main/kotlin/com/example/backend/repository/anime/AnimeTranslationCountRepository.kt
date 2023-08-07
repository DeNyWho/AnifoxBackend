package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeEpisodeTranslationCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeTranslationCountRepository : JpaRepository<AnimeEpisodeTranslationCount, String>
