package club.anifox.backend.config.security

import club.anifox.backend.config.security.jwt.JwtAuthConverter
import club.anifox.backend.config.security.keycloak.KeycloakOidcUserService
import club.anifox.backend.config.security.oauth2.CustomAuthenticationSuccessHandler
import club.anifox.backend.domain.enums.user.RoleName
import club.anifox.backend.util.UnauthorizedEntryPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig
@Autowired
constructor(
    private val jwtAuthConverter: JwtAuthConverter,
    @Value("\${keycloak.auth-server-url}") private val authUrl: String,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.resource}") private val clientId: String,
    @Value("\${keycloak.credentials.secret}") private val secret: String,
    @Value("\${domain}") private val domain: String,
    private val keycloakOidcUserService: KeycloakOidcUserService,
    private val customAuthenticationSuccessHandler: CustomAuthenticationSuccessHandler,
) {
    private fun keycloakClientRegistration(): ClientRegistration {
        return ClientRegistration.withRegistrationId(clientId)
            .clientId(clientId)
            .clientSecret(secret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri("${authUrl}realms/$realm/protocol/openid-connect/auth")
            .tokenUri("${authUrl}realms/$realm/protocol/openid-connect/token")
            .userInfoUri("${authUrl}realms/$realm/protocol/openid-connect/userinfo")
            .jwkSetUri("${authUrl}realms/$realm/protocol/openid-connect/certs")
            .issuerUri("${authUrl}realms/$realm")
            .redirectUri("https://$domain/login/oauth2/code/$clientId")
            .userNameAttributeName("preferred_username")
            .scope("openid")
            .build()
    }

    @Bean
    fun clientRegistrationRepository() =
        InMemoryClientRegistrationRepository(
            keycloakClientRegistration(),
        )

    @Bean
    fun authorizedClientService() =
        InMemoryOAuth2AuthorizedClientService(
            clientRegistrationRepository(),
        )

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun unauthorizedEntryPoint() = UnauthorizedEntryPoint()

    @Bean
    @Order(1)
    fun swaggerFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher(
                OrRequestMatcher(
                    AntPathRequestMatcher("/springdoc/**"),
                    AntPathRequestMatcher("/oauth2/**"),
                    AntPathRequestMatcher("/login/**"),
                ),
            )
            .authorizeHttpRequests { it.anyRequest().hasRole(RoleName.DEV.name) }
            .oauth2Login {
                it
                    .clientRegistrationRepository(clientRegistrationRepository())
                    .authorizedClientService(authorizedClientService())
                    .loginPage("https://$domain/oauth2/authorization/$clientId")
                    .userInfoEndpoint { keycloakOidcUserService }
            }
            .csrf { it.disable() }
            .build()

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatchers { it.requestMatchers("/api/**", "/images/**") }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    AntPathRequestMatcher("/api/anime/block"), // Явное указание путей
                ).hasRole(RoleName.ADMIN.name)
                auth.requestMatchers(
                    AntPathRequestMatcher("/actuator/**"),
                    AntPathRequestMatcher("/api/anime/**"),
                    AntPathRequestMatcher("/api/characters/**"),
                    AntPathRequestMatcher("/api/test/**"),
                    AntPathRequestMatcher("/api/shikimori/**"),
                    AntPathRequestMatcher("/images/**"),
                    AntPathRequestMatcher("/oauth2/**"),
                    AntPathRequestMatcher("/api/auth/**"),
                    AntPathRequestMatcher("/api/auth/oauth2/code/**"),
                ).permitAll()
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
                it.sessionCreationPolicy(SessionCreationPolicy.NEVER)
            }
            .formLogin {
                it.disable()
            }
            .csrf { it.disable() }
            .cors { it.disable() }
        return http.build()
    }
}
