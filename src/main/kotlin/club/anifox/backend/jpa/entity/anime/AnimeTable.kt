package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import jakarta.persistence.Cacheable
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "anime", schema = "anime")
@Cacheable(true)
data class AnimeTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val type: String = "",
    @Column(columnDefinition = "TEXT")
    val url: String = "",
    @Column(columnDefinition = "TEXT")
    val link: String = "",
    @Column(columnDefinition = "TEXT")
    val title: String = "",
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_titleEnglish", schema = "anime")
    @Column(columnDefinition = "text")
    val titleEn: MutableList<String?> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_titleJapan", schema = "anime")
    @Column(columnDefinition = "text")
    val titleJapan: MutableList<String?> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_synonyms", schema = "anime")
    @Column(columnDefinition = "text")
    val synonyms: MutableList<String?> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_otherTitles", schema = "anime")
    @Column(columnDefinition = "text")
    val otherTitles: MutableList<String> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_similar", schema = "anime")
    @BatchSize(size = 10)
    val similarAnime: MutableList<Int> = mutableListOf(),
    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @JoinTable(schema = "anime")
    @BatchSize(size = 10)
    val related: MutableSet<AnimeRelatedTable> = mutableSetOf(),
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(schema = "anime")
    @BatchSize(size = 10)
    val episodes: MutableSet<AnimeEpisodeTable> = mutableSetOf(),
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(schema = "anime")
    @BatchSize(size = 10)
    val translationsCountEpisodes: MutableSet<AnimeEpisodeTranslationCountTable> = mutableSetOf(),
    @OneToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var ids: AnimeIdsTable = AnimeIdsTable(),
    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
    )
    @JoinTable(
        name = "anime_music",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "music_id", referencedColumnName = "id")],
        schema = "anime",
    )
    val music: MutableSet<AnimeMusicTable> = mutableSetOf(),
    val year: Int = 0,
    var nextEpisode: LocalDateTime? = null,
    val episodesCount: Int = 0,
    var episodesAires: Int = 0,
    val shikimoriId: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val airedAt: LocalDate = LocalDate.now(),
    val releasedAt: LocalDate = LocalDate.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "anime")
    @BatchSize(size = 20)
    val translations: MutableSet<AnimeTranslationTable> = mutableSetOf(),
    var status: String = "",
    @Column(columnDefinition = "TEXT")
    val description: String = "",
    @OneToOne(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var images: AnimeImagesTable = AnimeImagesTable(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_screenshots", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val screenshots: MutableList<String> = mutableListOf(),
    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @JoinTable(
        name = "anime_genres",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var genres: MutableSet<AnimeGenreTable> = mutableSetOf(),
    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @JoinTable(
        name = "anime_media",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "media_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var media: MutableSet<AnimeMediaTable> = mutableSetOf(),
    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @JoinTable(
        name = "anime_studios",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "studio_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var studios: MutableSet<AnimeStudioTable> = mutableSetOf(),
    val shikimoriRating: Double = 0.0,
    val shikimoriVotes: Int = 0,
    val ratingMpa: String = "",
    val minimalAge: Int = 0,
    val season: String = "",
    val accentColor: String = "",
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val favorites: MutableSet<UserFavoriteAnimeTable> = mutableSetOf(),
    @Column(nullable = true)
    var totalRating: Double? = null,
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val rating: MutableSet<AnimeRatingTable> = mutableSetOf(),
) {
    fun addTranslation(translation: List<AnimeTranslationTable>): AnimeTable {
        translations.addAll(translation)
        translation.forEach { newTranslation ->
            val existingTranslation = translations.find { it.id == newTranslation.id }
            if (existingTranslation == null) {
                translations.add(newTranslation)
            }
        }
        return this
    }

    fun addTranslationCount(translation: List<AnimeEpisodeTranslationCountTable>): AnimeTable {
        translation.forEach { newTranslation ->
            val existingTranslation = translationsCountEpisodes.find { it.translation == newTranslation.translation }
            if (existingTranslation == null) {
                translationsCountEpisodes.add(newTranslation)
            } else {
                existingTranslation.countEpisodes = newTranslation.countEpisodes
            }
        }
        return this
    }

    fun addRelated(relatedList: List<AnimeRelatedTable>): AnimeTable {
        related.addAll(relatedList)
        return this
    }

    fun addMediaAll(mediaAll: List<AnimeMediaTable>): AnimeTable {
        media.addAll(mediaAll)
        return this
    }

    fun addEpisodesAll(episodesAll: List<AnimeEpisodeTable>): AnimeTable {
        episodesAll.forEach { newEpisode ->
            val existingEpisode = episodes.find { it.number == newEpisode.number }
            if (existingEpisode == null || existingEpisode != newEpisode) {
                existingEpisode?.apply {
                    translations = newEpisode.translations
                    title = newEpisode.title
                    titleEn = newEpisode.titleEn
                    description = newEpisode.description
                    descriptionEn = newEpisode.descriptionEn
                    image = newEpisode.image
                    aired = newEpisode.aired
                    filler = newEpisode.filler
                    recap = newEpisode.recap
                } ?: episodes.add(newEpisode)
            }
        }
        return this
    }

    fun addAllAnimeGenre(genre: List<AnimeGenreTable>): AnimeTable {
        genres.addAll(genre)
        return this
    }

    fun addAllMusic(musicT: List<AnimeMusicTable>): AnimeTable {
        music.addAll(musicT)
        return this
    }

    fun addAllAnimeStudios(studio: List<AnimeStudioTable>): AnimeTable {
        studios.addAll(studio)
        return this
    }
}
