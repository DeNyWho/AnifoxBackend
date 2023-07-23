package com.example.backend.models.keycloak

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyCloakTokenRefresh(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("not-before-policy")
    val notBeforePolicy: Int,
    @SerialName("session_state")
    val sessionState: String,
    @SerialName("scope")
    val scope: String
)