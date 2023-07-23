package com.example.backend.models.users

data class TokenResponse(
    val accessToken: String,
    val accessExpires: Long,
    val refreshToken: String,
    val refreshExpires: Long,
)