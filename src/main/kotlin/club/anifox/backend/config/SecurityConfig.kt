package club.anifox.backend.config

import club.anifox.backend.config.jwt.JwtAuthConverter
import club.anifox.backend.util.UnauthorizedEntryPoint
import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider
import org.keycloak.adapters.springsecurity.management.HttpSessionManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy

@Configuration
@EnableWebSecurity
@KeycloakConfiguration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig {

    @Autowired
    private lateinit var jwtAuthConverter: JwtAuthConverter

    @Autowired
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        val grantedAuthorityMapper = SimpleAuthorityMapper()
        grantedAuthorityMapper.setPrefix(AUTHORITY_MAPPER_PREFIX)
        grantedAuthorityMapper.setConvertToUpperCase(true)
        val keycloakAuthenticationProvider = KeycloakAuthenticationProvider()
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(grantedAuthorityMapper)
        auth.authenticationProvider(keycloakAuthenticationProvider)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun sessionRegistry(): SessionRegistry {
        return SessionRegistryImpl()
    }

    @Bean
    @ConditionalOnMissingBean(HttpSessionManager::class)
    fun httpSessionManager(): HttpSessionManager {
        return HttpSessionManager()
    }

    @Bean
    fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
        return NullAuthenticatedSessionStrategy()
    }

    @Bean
    fun KeycloakConfigResolver(): KeycloakConfigResolver {
        return KeycloakSpringBootConfigResolver()
    }

    @Bean
    fun unauthorizedEntryPoint() = UnauthorizedEntryPoint()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
//            .requiresChannel { requiresChannel ->
//                requiresChannel
//                    .anyRequest()
//                    .requiresSecure()
//            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .csrf {
                it.disable()
            }
            .cors {
                it.disable()
            }
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthConverter)
                }
            }
            .securityMatchers {
                // only apply security to those endpoints
                it.requestMatchers(
                    "/api/users/**",
                    "/api/**/admin/**",
                    "/api/account/**",
                )
                it.requestMatchers(EndpointRequest.toAnyEndpoint())
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
                auth.requestMatchers("/api/**/admin/**").hasAnyRole(ADMIN)
                auth.requestMatchers("/api/users/**").hasAnyRole(ADMIN, USER)
                auth.requestMatchers("/api/account/**").hasAnyRole(ADMIN, USER)
                auth.anyRequest().authenticated()
            }
//            .exceptionHandling {
//                it.authenticationEntryPoint(unauthorizedEntryPoint())
//            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
        return http.build()
    }

    companion object {
        private const val AUTHORITY_MAPPER_PREFIX = "ROLE_"
        private const val ADMIN = "ADMIN"
        private const val USER = "USER"
    }
}
