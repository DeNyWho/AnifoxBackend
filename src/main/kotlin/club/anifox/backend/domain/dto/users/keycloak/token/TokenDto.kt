package club.anifox.backend.domain.dto.users.keycloak.token

import club.anifox.backend.domain.dto.users.keycloak.realm.ResourceAccessDto
import club.anifox.backend.domain.dto.users.keycloak.role.RolesDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TokenDto(
    @SerialName("exp") var exp: Int? = null,
    @SerialName("iat") var iat: Int? = null,
    @SerialName("jti") var jti: String? = null,
    @SerialName("iss") var iss: String? = null,
    @SerialName("aud") var aud: String? = null,
    @SerialName("sub") var sub: String? = null,
    @SerialName("typ") var typ: String? = null,
    @SerialName("azp") var azp: String? = null,
    @Transient
    @SerialName("session_state")
    var sessionState: String? = null,
    @SerialName("acr") var acr: String? = null,
    @SerialName("allowed-origins") var allowedOrigins: List<String> = emptyList(),
    @SerialName("realm_access") var realmAccess: RolesDto? = RolesDto(),
    @SerialName("resource_access") var resourceAccess: ResourceAccessDto? = ResourceAccessDto(),
    @SerialName("scope") var scope: String? = null,
    @SerialName("sid") var sid: String? = null,
    @SerialName("email_verified") var emailVerified: Boolean? = null,
    @SerialName("preferred_username") var preferredUsername: String? = null,
    @SerialName("email") var email: String? = null,
)
