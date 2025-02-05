package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeIdsTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.common.AnimeRelatedTable
import club.anifox.backend.jpa.entity.anime.common.AnimeSimilarTable
import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.common.AnimeVideoTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeScheduleTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeEpisodeTranslationCountTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.entity.user.UserFavoriteAnimeTable
import jakarta.persistence.Cacheable
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "anime", schema = "anime")
@Cacheable(true)
@DynamicUpdate
data class AnimeTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    val type: AnimeType = AnimeType.Tv,
    @Column(columnDefinition = "TEXT")
    val url: String = "",
    @Column(columnDefinition = "TEXT")
    val playerLink: String? = null,
    @Column(columnDefinition = "TEXT")
    val title: String = "",
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_titleEnglish", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val titleEn: MutableList<String> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_titleJapan", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val titleJapan: MutableList<String> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_synonyms", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val synonyms: MutableList<String> = mutableListOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_otherTitles", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val titleOther: MutableList<String> = mutableListOf(),
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 10)
    @JoinTable(schema = "anime")
    val episodes: MutableSet<AnimeEpisodeTable> = mutableSetOf(),
    @Column(nullable = true)
    var duration: Int? = null,
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 10)
    @JoinTable(schema = "anime")
    val translationsCountEpisodes: MutableSet<AnimeEpisodeTranslationCountTable> = mutableSetOf(),
    @OneToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var ids: AnimeIdsTable = AnimeIdsTable(),
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 10)
    val similar: MutableSet<AnimeSimilarTable> = mutableSetOf(),
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 10)
    val related: MutableSet<AnimeRelatedTable> = mutableSetOf(),
    val year: Int = 0,
    var nextEpisode: LocalDateTime? = null,
    @OneToOne(
        mappedBy = "anime",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true,
    )
    var schedule: AnimeEpisodeScheduleTable? = null,
    var episodesCount: Int? = null,
    var episodesAired: Int? = null,
    val shikimoriId: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val airedOn: LocalDate = LocalDate.now(),
    val releasedOn: LocalDate? = null,
    var updatedAt: LocalDateTime? = null,
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 10)
    @JoinTable(schema = "anime")
    val externalLinks: MutableSet<AnimeExternalLinksTable> = mutableSetOf(),
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "anime")
    @BatchSize(size = 20)
    val translations: MutableSet<AnimeTranslationTable> = mutableSetOf(),
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var status: AnimeStatus = AnimeStatus.Ongoing,
    @Column(columnDefinition = "TEXT")
    var description: String = "",
    @Column(columnDefinition = "TEXT", nullable = true)
    var franchise: String? = null,
    @OneToOne(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var images: AnimeImagesTable = AnimeImagesTable(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_screenshots", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 20)
    val screenshots: MutableList<String> = mutableListOf(),
    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @BatchSize(size = 10)
    @JoinTable(
        name = "anime_genres",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var genres: MutableSet<AnimeGenreTable> = mutableSetOf(),
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 10)
    val characterRoles: MutableSet<AnimeCharacterRoleTable> = mutableSetOf(),
    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @BatchSize(size = 10)
    @JoinTable(
        name = "anime_video",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "video_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var videos: MutableSet<AnimeVideoTable> = mutableSetOf(),
    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    @BatchSize(size = 10)
    @JoinTable(
        name = "anime_studios",
        joinColumns = [JoinColumn(name = "anime_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "studio_id", referencedColumnName = "id")],
        schema = "anime",
    )
    var studios: MutableSet<AnimeStudioTable> = mutableSetOf(),
    var shikimoriRating: Double = 0.0,
    var shikimoriVotes: Int = 0,
    val ratingMpa: String = "",
    val minimalAge: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    val season: AnimeSeason = AnimeSeason.Summer,
    val accentColor: String = "",
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 10)
    val favorites: MutableSet<UserFavoriteAnimeTable> = mutableSetOf(),
    @Column(nullable = true)
    var totalRating: Double? = null,
    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 10)
    val rating: MutableSet<AnimeRatingTable> = mutableSetOf(),
) {
    fun updateEpisodeSchedule(nextEpisodeDate: LocalDateTime?): AnimeTable {
        if (nextEpisodeDate != null) {
            val dayOfWeek = nextEpisodeDate.dayOfWeek

            if (this.schedule != null) {
                if (this.schedule?.nextEpisodeDate != nextEpisodeDate) {
                    this.schedule = null
                } else {
                    return this
                }
            }

            val newSchedule = AnimeEpisodeScheduleTable(
                anime = this,
                nextEpisodeDate = nextEpisodeDate,
                dayOfWeek = dayOfWeek,
            )
            this.schedule = newSchedule
        } else {
            this.schedule = null
        }

        return this
    }

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

    fun addExternalLinks(externalLinksList: List<AnimeExternalLinksTable>): AnimeTable {
        this.externalLinks.addAll(externalLinksList)
        return this
    }

    fun addSimilar(similarList: List<AnimeSimilarTable>): AnimeTable {
        this.similar.addAll(similarList)
        return this
    }

    fun addRelation(relationList: List<AnimeRelatedTable>): AnimeTable {
        this.related.addAll(relationList)
        return this
    }

    fun addVideos(videos: List<AnimeVideoTable>): AnimeTable {
        this.videos.addAll(videos)
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

    fun addAllAnimeStudios(studio: List<AnimeStudioTable>): AnimeTable {
        studios.addAll(studio)
        return this
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeTable) return false
        return id == other.id
    }

    override fun toString(): String {
        return "AnimeTable(id='$id')"
    }
}
