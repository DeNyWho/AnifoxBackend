package com.example.backend.jpa.manga

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "types", schema = "manga")
data class MangaTypes(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    val type: String = "",
    @Column(nullable = true)
    val year: Int? = null,
    @Column(nullable = true)
    val status: String? = null,
    @Column(nullable = true)
    val limitation: String? = null
)