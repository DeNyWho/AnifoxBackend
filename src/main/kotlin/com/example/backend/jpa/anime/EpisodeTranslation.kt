package com.example.backend.jpa.anime

import javax.persistence.*

@Entity
@Table(name = "anime_translation_episode_mapping", schema = "anime")
data class AnimeTranslationEpisodeMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    val animeTranslation: AnimeTranslationTable = AnimeTranslationTable(),

    @ManyToOne
    val animeEpisode: AnimeEpisodeTable = AnimeEpisodeTable()
)