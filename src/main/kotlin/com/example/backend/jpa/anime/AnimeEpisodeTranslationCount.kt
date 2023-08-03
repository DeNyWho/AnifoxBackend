package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.*


@Entity
@Table(name = "episode_translation_count", schema = "anime")
data class AnimeEpisodeTranslationCount(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", referencedColumnName = "id")
    val translation: AnimeTranslationTable = AnimeTranslationTable(),

    val countEpisodes: Int = 0
)