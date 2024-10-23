package club.anifox.backend.config.security.keycloak

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service
class KeycloakOidcUserService(
    private val keycloakOidcUserGrantedAuthoritiesConverter: KeycloakOidcUserGrantedAuthoritiesConverter,
) : OidcUserService() {
    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = super.loadUser(userRequest)
        val mappedAuthorities = keycloakOidcUserGrantedAuthoritiesConverter.toGrantedAuthorities(userRequest, oidcUser)

        val providerDetails = userRequest.clientRegistration.providerDetails
        val userNameAttributeName = providerDetails.userInfoEndpoint.userNameAttributeName
        return if (StringUtils.hasText(userNameAttributeName)) {
            DefaultOidcUser(mappedAuthorities, oidcUser.idToken, oidcUser.userInfo, userNameAttributeName)
        } else {
            DefaultOidcUser(mappedAuthorities, oidcUser.idToken, oidcUser.userInfo)
        }
    }
}
