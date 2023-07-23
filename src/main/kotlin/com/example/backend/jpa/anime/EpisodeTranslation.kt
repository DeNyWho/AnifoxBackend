package com.example.backend.jpa.anime

import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "episode_translation", schema = "anime")
data class EpisodeTranslation(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne
    @JoinColumn(name = "translation_id", referencedColumnName = "id")
    val translation: AnimeTranslationTable = AnimeTranslationTable(),

    @Column(nullable = false, columnDefinition = "TEXT")
    val link: String = ""
)