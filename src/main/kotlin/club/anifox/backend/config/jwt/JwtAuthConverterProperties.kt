package club.anifox.backend.config.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties("jwt.auth.converter")
@Validated
data class JwtAuthConverterProperties(
    var resourceId: String? = null,
    var principalAttribute: String? = null,
)
