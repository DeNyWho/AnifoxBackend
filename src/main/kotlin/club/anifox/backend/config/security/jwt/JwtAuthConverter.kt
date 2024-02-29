package club.anifox.backend.config.security.jwt

import club.anifox.backend.domain.exception.common.UnauthorizedException
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component

@Component
class JwtAuthConverter(private val properties: JwtAuthConverterProperties) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken? {
        try {
            val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()

            val authorities = (
                jwtGrantedAuthoritiesConverter.convert(jwt)!!.asSequence() +
                    extractResourceRoles(jwt).asSequence()
                ).toSet()
                .map { SimpleGrantedAuthority("$it") }
            return JwtAuthenticationToken(jwt, authorities.toList(), getPrincipalClaimName(jwt))
        } catch (e: Exception) {
            throw UnauthorizedException()
        }
    }

    private fun getPrincipalClaimName(jwt: Jwt): String {
        var claimName = JwtClaimNames.SUB
        if (properties.principalAttribute != null) {
            claimName = properties.principalAttribute!!
        }
        return jwt.getClaim(claimName)
    }

    private fun extractResourceRoles(jwt: Jwt): Collection<String> {
        val resourceAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        val resourceRoles = resourceAccess?.get("roles") as? Collection<String>

        return resourceRoles ?: emptyList()
    }
}
