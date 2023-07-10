package com.example.backend.jpa.anime

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "anime_ids", schema = "anime")
data class AnimeIds(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val aniDb: Int? = null,
    val aniList: Int? = null,
    val animePlanet: String? = null,
    val aniSearch: Int? = null,
    val imdb: String? = null,
    val kitsu: Int? = null,
    val liveChart: Int? = null,
    val notifyMoe: String? = null,
    val thetvdb: Int? = null,
    val myAnimeList: Int? = null,
)