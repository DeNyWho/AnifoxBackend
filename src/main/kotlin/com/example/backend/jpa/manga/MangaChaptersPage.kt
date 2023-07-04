package com.example.backend.jpa.manga

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "chapters_page", schema = "manga")
data class MangaChaptersPage(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    val imagePageUrl: String = ""
)