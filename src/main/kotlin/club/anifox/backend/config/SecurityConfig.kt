package club.anifox.backend.config

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
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy

@Configuration
@EnableWebSecurity
@KeycloakConfiguration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig {

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
    fun clientRegistrationRepository(): InMemoryClientRegistrationRepository {
        val clientRegistration = ClientRegistration.withRegistrationId("shikimori")
            .clientId("YOUR_CLIENT_ID")
            .clientSecret("YOUR_CLIENT_SECRET")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://shikimori.one/oauth/authorize")
            .tokenUri("https://shikimori.one/oauth/token")
            .userInfoUri("https://shikimori.one/api/users/whoami")
            .userNameAttributeName("id")
            .clientName("Shikimori")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .build()

        return InMemoryClientRegistrationRepository(clientRegistration)
    }

    @Bean
    fun KeycloakConfigResolver(): KeycloakConfigResolver {
        return KeycloakSpringBootConfigResolver()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .requiresChannel { requiresChannel ->
                requiresChannel
                    .anyRequest()
                    .requiresSecure()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .csrf {
                it.disable()
            }
            .cors {
                it.disable()
            }
//            .oauth2Login {
//                it.defaultSuccessUrl("/callback")
//            }
            .securityMatchers {
                // only apply security to those endpoints
                it.requestMatchers(
                    "/api/users/**",
                    "/api/**/admin/**",
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
                auth.requestMatchers("/api/**/admin/**").hasAuthority(ADMIN)
                auth.requestMatchers("/api/users/**").hasAnyAuthority(ADMIN, USER)
                auth.anyRequest().authenticated()
            }
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
