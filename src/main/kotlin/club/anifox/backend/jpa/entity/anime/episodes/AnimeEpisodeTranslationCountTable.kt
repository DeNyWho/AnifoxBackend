package club.anifox.backend.jpa.entity.anime.episodes

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.util.*

@Entity
@BatchSize(size = 10)
@Table(name = "episode_translation_count", schema = "anime")
data class AnimeEpisodeTranslationCountTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", referencedColumnName = "id")
    val translation: AnimeTranslationTable = AnimeTranslationTable(),
    var countEpisodes: Int = 0,
)
