package club.anifox.backend.jpa.entity.anime.episodes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "episode_translation", schema = "anime")
data class EpisodeTranslationTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", referencedColumnName = "id")
    val translation: AnimeTranslationTable = AnimeTranslationTable(),
    @Column(nullable = false, columnDefinition = "TEXT")
    var link: String = "",
)
