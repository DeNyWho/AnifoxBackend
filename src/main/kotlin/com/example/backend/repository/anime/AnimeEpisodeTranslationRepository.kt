package com.example.backend.repository.anime

import com.example.backend.jpa.anime.EpisodeTranslation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeEpisodeTranslationRepository : JpaRepository<EpisodeTranslation, String>