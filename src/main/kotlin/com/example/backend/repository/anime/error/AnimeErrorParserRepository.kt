package com.example.backend.repository.anime.error

import com.example.backend.jpa.anime.AnimeErrorParserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeErrorParserRepository: JpaRepository<AnimeErrorParserTable, String>