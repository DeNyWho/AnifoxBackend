package com.example.backend.jpa.anime

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

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val translations: MutableSet<AnimeTranslationTable> = mutableSetOf(),
) {
    fun addTranslation(translation: AnimeTranslationTable): AnimeEpisodeTable {
        translations.add(translation)
        return this
    }
}