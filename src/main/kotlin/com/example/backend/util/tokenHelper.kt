package com.example.backend.util

import com.example.backend.models.users.Token
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenHelper {
    private val json = Json { ignoreUnknownKeys = true }

    fun getTokenInfo(token: String) = json.decodeFromString<Token>(decodeJWT(token))

    fun decodeJWT(token: String): String {
        val parts = token.split(".")
        val payload = parts[1]
        val decoded = Base64.getDecoder().decode(payload)
        return String(decoded)
    }
}