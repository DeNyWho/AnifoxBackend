package club.anifox.backend.domain.dto.users.keycloak.role

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RolesDto(
    @SerialName("roles") var roles: List<String> = emptyList(),
)
