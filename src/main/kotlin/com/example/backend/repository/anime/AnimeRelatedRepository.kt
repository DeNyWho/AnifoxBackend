package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeRelatedTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeRelatedRepository : JpaRepository<AnimeRelatedTable, String>