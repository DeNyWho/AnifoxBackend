package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min


@Entity
@Table(name = "rating_count", schema = "anime")
data class AnimeRatingCount(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id")
    val anime: AnimeTable = AnimeTable(),

    @field:Min(1)
    @field:Max(10)
    val rating: Int = 0,

    var count: Int = 0
)