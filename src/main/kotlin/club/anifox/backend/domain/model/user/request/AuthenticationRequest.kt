package club.anifox.backend.domain.model.user.request

data class AuthenticationRequest(
    val userIdentifier: String,
    val password: String,
)
