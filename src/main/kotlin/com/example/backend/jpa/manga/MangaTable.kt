package com.example.backend.jpa.manga

import com.example.backend.jpa.user.UserFavoriteAnime
import com.example.backend.jpa.user.UserFavouriteManga
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "manga", schema = "manga")
data class MangaTable (
    @Id
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var image: String = "",
    var url: String = "",
    @Column(columnDefinition = "TEXT")
    var description: String = "",
    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH]
    )
    @JoinTable(
        name = "manga_genres",
        joinColumns = [JoinColumn(name = "manga_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genres_id", referencedColumnName = "id")],
        schema = "manga",
    )
    var genres: MutableSet<MangaGenre> = mutableSetOf(),
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var types: MangaTypes = MangaTypes(),
    var chaptersCount: Int = 0,
    val views: Int = 0,
    @Column(nullable = true)
    var totalRating: Double? = null,
    @OneToMany(
        mappedBy = "manga",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val rating: MutableSet<MangaRating> = mutableSetOf(),
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(nullable = true)
    @CollectionTable(name = "manga_linked", schema = "manga")
    var linked: MutableSet<String> = mutableSetOf(),
    val updateTime: LocalDateTime = LocalDateTime.now(),
    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH]
    )
    @JoinTable(schema = "manga")
    var chapters: MutableSet<MangaChapters> = mutableSetOf(),
    @OneToMany(
        mappedBy = "manga",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val favorites: MutableSet<UserFavouriteManga> = mutableSetOf(),
) {
    fun addMangaLinked(linkedTemp: String): MangaTable{
        linked.add(linkedTemp)
        return this
    }

    fun addMangaChapters(chaptersTemp: List<MangaChapters>){
        chapters.addAll(chaptersTemp)
    }

    fun addMangaGenre(genreTemp: MangaGenre): MangaTable{
        genres.add(genreTemp)
        return this
    }
}