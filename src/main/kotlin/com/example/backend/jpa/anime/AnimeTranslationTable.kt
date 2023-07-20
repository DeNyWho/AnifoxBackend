package com.example.backend.jpa.anime

import javax.persistence.*


@Entity
@Table(name = "translation", schema = "anime")
data class AnimeTranslationTable(
    @Id
    val id: Int = 0,
    val title: String = "",
    val voice: String = "",
)