package com.example.backend.jpa.manga

import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min


@Entity
@Table(name = "rating_count", schema = "manga")
data class MangaRatingCount(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manga_id")
    val manga: MangaTable = MangaTable(),

    @field:Min(1)
    @field:Max(10)
    val rating: Int = 0,

    var count: Int = 0
)