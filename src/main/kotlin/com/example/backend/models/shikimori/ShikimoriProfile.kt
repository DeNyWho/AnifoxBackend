package com.example.backend.models.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Serial

@Serializable
data class ShikimoriProfile(
    @SerialName("id")
    val id: Int,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("avatar")
    val avatar: String,
)