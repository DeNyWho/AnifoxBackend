package com.example.backend.jpa.anime

import kotlinx.serialization.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table


@Entity
@Table(name = "anime_images", schema = "anime")
@Serializable
data class AnimeImages(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(columnDefinition = "TEXT")
    val large: String = "",
    @Column(columnDefinition = "TEXT")
    val medium: String = "",
)