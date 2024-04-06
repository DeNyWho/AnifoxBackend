package club.anifox.backend.config.security.keycloak

import org.springframework.stereotype.Component

@Component
class KeycloakJwtClaimsAuthoritiesConverter {
    fun convert(jwtClaims: Map<String, Any>): Collection<String> {
        val realmRoles = convertRealmRoles(jwtClaims)
        val resourceRoles = convertResourceRoles(jwtClaims)
        return realmRoles + resourceRoles
    }

    private fun convertRealmRoles(jwtClaims: Map<String, Any>): List<String> {
        val realmAccess = jwtClaims[CLAIM_REALM_ACCESS] as? Map<*, *> ?: return emptyList()
        val roles = realmAccess[KEY_ROLES] as? Collection<*> ?: return emptyList()
        return roles.mapNotNull { role -> "$role" }
    }

    private fun convertResourceRoles(jwtClaims: Map<String, Any>): List<String> {
        val accessByResource = jwtClaims[CLAIM_RESOURCE_ACCESS] as? Map<*, *> ?: return emptyList()
        val roles = accessByResource[KEY_ROLES] as? Collection<*> ?: return emptyList()
        return roles.mapNotNull { it.toString() }
    }

    companion object {
        private const val CLAIM_REALM_ACCESS = "realm_access"
        private const val CLAIM_RESOURCE_ACCESS = "resource_access"
        private const val KEY_ROLES = "roles"
    }
}
