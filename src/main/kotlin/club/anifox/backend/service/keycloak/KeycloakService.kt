package club.anifox.backend.service.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KeycloakService(
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm}") private val realm: String,
) {
    fun getCredentialRepresentation(password: String?): CredentialRepresentation {
        val passwordCred = CredentialRepresentation()
        return passwordCred.apply {
            isTemporary = false
            type = CredentialRepresentation.PASSWORD
            value = password
        }
    }

    fun findByEmail(email: String): UserRepresentation? =
        keycloak
            .realm(realm)
            .users()
            .searchByEmail(email, true).firstOrNull()

    fun findByUsername(username: String): UserRepresentation? =
        keycloak
            .realm(realm)
            .users()
            .searchByUsername(username, true).firstOrNull()

    fun findByUserID(id: String): UserRepresentation? =
        try {
            println("ZXCXZ = $realm")
            keycloak
                .realm(realm)
                .users()
                .get(id)
                .toRepresentation()
        } catch (e: Exception) {
            null
        }
}
