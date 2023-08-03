package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeBlockedTable
import org.springframework.data.jpa.repository.JpaRepository

interface AnimeBlockedRepository: JpaRepository<AnimeBlockedTable, Int>