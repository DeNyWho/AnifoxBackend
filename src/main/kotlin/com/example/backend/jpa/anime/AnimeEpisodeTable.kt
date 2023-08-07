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
    var title: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    var titleEn: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    var descriptionEn: String? = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = "",

    @Column(nullable = false)
    val number: Int = 0,

    @Column(nullable = true)
    var image: String? = "",

    @Column(nullable = true)
    var aired: LocalDate? = LocalDate.now(),

    var filler: Boolean = false,

    var recap: Boolean = false,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(schema = "anime")
    @BatchSize(size = 20)
    var translations: MutableSet<EpisodeTranslation> = mutableSetOf()
) {
    fun addTranslation(translation: EpisodeTranslation): AnimeEpisodeTable {
        val existingTranslation = translations.find { translation.translation == it.translation }
        if(existingTranslation == null) {
            translations.add(translation)
        }
        return this
    }
}