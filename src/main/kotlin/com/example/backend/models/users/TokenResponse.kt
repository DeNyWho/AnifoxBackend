package com.example.backend.models.users

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)