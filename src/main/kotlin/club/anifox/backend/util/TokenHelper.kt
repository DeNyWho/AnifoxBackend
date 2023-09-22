package club.anifox.backend.util

import club.anifox.backend.domain.dto.users.keycloak.token.TokenDto
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenHelper {
    private val json = Json { ignoreUnknownKeys = true }

    fun getTokenInfo(token: String) = json.decodeFromString<TokenDto>(decodeJWT(token))

    fun decodeJWT(token: String): String {
        val parts = token.split(".")
        val payload = parts[1]
        val decoded = Base64.getDecoder().decode(payload)
        return String(decoded)
    }
}
