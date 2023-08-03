package com.example.backend.jpa.anime

import com.example.backend.models.anime.AnimeBlockedType
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "blocked", schema = "anime")
data class AnimeBlockedTable(
    @Id
    val shikimoriID: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var type: AnimeBlockedType = AnimeBlockedType.ALL,
)