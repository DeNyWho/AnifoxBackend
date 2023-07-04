package com.example.backend.jpa.manga

import org.hibernate.annotations.GenericGenerator
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "chapters", schema = "manga")
data class MangaChapters(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    var id: UUID? = null,
    val title: String = "",
    val urlCode: Int = 0,
    val date: LocalDate = LocalDate.now(),
    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL]
    )
    @JoinTable(
        schema = "manga",
        name = "chapters_mangachapterspage", // Имя таблицы соединения
        joinColumns = [JoinColumn(name = "mangachapters_id")], // Имя столбца для связи с "MangaChapters"
        inverseJoinColumns = [JoinColumn(name = "mangachapterspage_id")] // Имя столбца для связи с "MangaChaptersPage"
    )
    val mangaChaptersPage: MutableSet<MangaChaptersPage> = mutableSetOf()
) {
    fun addToMangaChaptersPage(chapter: MangaChaptersPage): MangaChapters {
        mangaChaptersPage.add(chapter)
        return this
    }
}