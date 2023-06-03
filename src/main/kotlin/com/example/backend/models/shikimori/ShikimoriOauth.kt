package com.example.backend.models.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriOauth(
    @SerialName("access_token")
    val accessToken: String = "",
    @SerialName("refresh_token")
    val refreshToken: String = "",
    @SerialName("expires_in")
    val expiresIn: Int = 0,
    @SerialName("created_at")
    val createdAt: Int = 0,

)