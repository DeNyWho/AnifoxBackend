package club.anifox.backend.domain.dto.users.keycloak.realm

import club.anifox.backend.domain.dto.users.keycloak.role.RolesDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourceAccessDto(
    @SerialName("account") var roles: RolesDto? = RolesDto(),
)
