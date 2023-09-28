package club.anifox.backend.domain.model.user.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val login: String,
    val email: String,
    val password: String,
    val nickname: String,
)
