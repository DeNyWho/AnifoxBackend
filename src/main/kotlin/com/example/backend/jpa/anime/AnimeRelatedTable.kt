package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "anime_related", schema = "anime")
data class AnimeRelatedTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = true)
    val type: String? = "",

    @Column(nullable = true)
    val typeEn: String? = "",

    val shikimoriId: Int = 0,

//    val anime: AnimeTable,


)