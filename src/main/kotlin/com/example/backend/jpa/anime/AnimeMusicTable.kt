package com.example.backend.jpa.anime

import kotlinx.serialization.Serializable
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "music", schema = "anime")
@Serializable
data class AnimeMusicTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val playerUrl: String = "",
    val name: String = "",
    val episodes: String = "",
    val hosting: String = "",
)