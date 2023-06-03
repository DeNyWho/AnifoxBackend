package com.example.backend.repository.anime

import com.example.backend.jpa.anime.AnimeMusicTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnimeMusicRepository : JpaRepository<AnimeMusicTable, String>