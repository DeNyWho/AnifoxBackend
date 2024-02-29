package club.anifox.backend.config.security

import club.anifox.backend.config.security.jwt.JwtAuthConverter
import club.anifox.backend.util.UnauthorizedEntryPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig @Autowired constructor(
    private val jwtAuthConverter: JwtAuthConverter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun unauthorizedEntryPoint() = UnauthorizedEntryPoint()

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf {
                it.disable()
            }
            .cors {
                it.disable()
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/anime/**",
                    "/api/test/**",
                    "/api/shikimori/**",
                    "/images/**",
                    "/api/auth/**",
                    "/api/auth/oauth2/code/**",
                    "/api/auth/shikimori/**",
                    "/swagger-ui/**",
                    "/v3/**",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/configuration/**",
                    "/swagger-ui/**",
                    "/webjars/**",
                ).permitAll()
                auth.requestMatchers(HttpMethod.GET, "/admin").hasRole(ADMIN)
                auth.requestMatchers(HttpMethod.GET, "/user").hasAnyRole(ADMIN, USER)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(unauthorizedEntryPoint())
            }
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthConverter)
                }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
        return http.build()
    }

    companion object {
        private const val ADMIN = "ADMIN"
        private const val USER = "USER"
    }
}
