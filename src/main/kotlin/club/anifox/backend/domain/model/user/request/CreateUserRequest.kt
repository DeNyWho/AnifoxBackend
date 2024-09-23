@file:UseSerializers(LocalDateSerializer::class)

package club.anifox.backend.domain.model.user.request

import club.anifox.backend.util.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

@Serializable
data class CreateUserRequest(
    val login: String,
    val email: String,
    val password: String,
    val birthday: LocalDate?,
)
