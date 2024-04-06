package club.anifox.backend.config.security.keycloak

import com.nimbusds.jwt.JWTParser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.stereotype.Component

@Component
class KeycloakOidcUserGrantedAuthoritiesConverter(
    private val keycloakJwtClaimsAuthoritiesConverter: KeycloakJwtClaimsAuthoritiesConverter,
) {
    fun toGrantedAuthorities(
        userRequest: OidcUserRequest,
        oidcUser: OidcUser,
    ): Collection<GrantedAuthority> {
        val jwt = JWTParser.parse(userRequest.accessToken.tokenValue)
        val claims = jwt.jwtClaimsSet.claims
        return keycloakJwtClaimsAuthoritiesConverter.convert(claims)
            .map { OidcUserAuthority(it, oidcUser.idToken, oidcUser.userInfo) }
    }
}
