package com.example.backend.jpa.anime

import org.hibernate.annotations.BatchSize
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

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

    @Column(nullable = false)
    val number: Int = 0,

    @Column(nullable = true)
    val image: String? = "",

    val aired: LocalDate = LocalDate.now(),

    val filler: Boolean = false,

    val recap: Boolean = false,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(schema = "anime")
    @BatchSize(size = 20)
    val translations: MutableSet<EpisodeTranslation> = mutableSetOf()
) {
    fun addTranslation(translation: EpisodeTranslation): AnimeEpisodeTable {
        translations.add(translation)
        return this
    }
}