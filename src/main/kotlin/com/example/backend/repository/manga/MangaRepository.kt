package com.example.backend.repository.manga

import com.example.backend.jpa.manga.MangaChapters
import com.example.backend.jpa.manga.MangaTable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface MangaRepository: JpaRepository<MangaTable, String> {

    @Query("Select m From MangaTable m where m.url = :url")
    fun mangaByUrl(url: String): Optional<MangaTable>

    @Query("SELECT c FROM MangaTable m JOIN m.chapters c WHERE m.id = :id")
    fun findMangaWithChapters(id: String, pageable: PageRequest): Optional<MutableSet<MangaChapters>>

    @Query("select m from MangaTable m where ((:status) is null or m.types.status = :status )" +
        " and(:status is null or m.types.status = :status)"
    )
    fun findMGenres(pageable: Pageable, @Param("status") status: String?): List<MangaTable>

    @Query("SELECT m FROM MangaTable m WHERE (:searchQuery IS NULL OR UPPER(m.title) LIKE CONCAT('%', UPPER(:searchQuery), '%'))")
    fun findManga(pageable: Pageable, @Param("searchQuery") searchQuery: String?): List<MangaTable>



}