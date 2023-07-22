package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "anime_parser", schema = "anime")
data class AnimeErrorParserTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(nullable = true)
    val message: String? = null,
    @Column(nullable = true)
    val cause: String? = null
)