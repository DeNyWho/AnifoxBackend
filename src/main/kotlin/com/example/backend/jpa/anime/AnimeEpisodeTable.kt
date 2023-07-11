package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "anime_episodes", schema = "anime")
data class AnimeEpisodeTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = true, columnDefinition = "TEXT")
    val title: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    val titleEn: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    val descriptionEn: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    val description: String? = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val link: String = "",

    @Column(nullable = false)
    val number: Int = 0,

    @Column(nullable = true)
    val image: String? = ""
)