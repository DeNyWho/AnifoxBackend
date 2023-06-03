package com.example.backend.jpa.anime

import com.example.backend.models.anime.AnimeMusicType
import kotlinx.serialization.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "music", schema = "anime")
@Serializable
data class AnimeMusicTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val name: String = "",
    val episodes: String = "",
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = true)
    var type: AnimeMusicType = AnimeMusicType.Opening,
    val hosting: String = "",
)