package club.anifox.backend.domain.model.user.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationRequest(
    @SerialName("user_identifier")
    val userIdentifier: String,
    val password: String,
)
