package club.anifox.backend.jpa.entity.anime.episodes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.time.LocalDate
import java.util.*

@Entity
@BatchSize(size = 10)
@Table(name = "episodes", schema = "anime")
data class AnimeEpisodeTable(
    @Id
    var id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, columnDefinition = "TEXT")
    var title: String = "",
    @Column(nullable = false, columnDefinition = "TEXT")
    var titleEn: String = "",
    @Column(nullable = true, columnDefinition = "TEXT")
    var descriptionEn: String? = "",
    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = "",
    @Column(nullable = false)
    var number: Int = 0,
    @Column(nullable = true)
    var duration: Int? = null,
    @Column(nullable = false)
    var image: String = "",
    @Column(nullable = true)
    var aired: LocalDate? = LocalDate.now(),
    var filler: Boolean = false,
    var recap: Boolean = false,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(schema = "anime")
    @BatchSize(size = 20)
    var translations: MutableSet<EpisodeTranslationTable> = mutableSetOf(),
) {
    fun addTranslation(translation: EpisodeTranslationTable): AnimeEpisodeTable {
        val existingTranslation = translations.find { it.translation == translation.translation }

        if (existingTranslation == null) {
            translations.add(translation)
        }
        return this
    }
}
