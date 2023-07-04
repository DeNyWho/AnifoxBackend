package com.example.backend.config

import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticatedActionsFilter
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter
import org.keycloak.adapters.springsecurity.filter.KeycloakSecurityContextRequestFilter
import org.keycloak.adapters.springsecurity.management.HttpSessionManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import javax.net.ssl.*


@Configuration
@EnableWebSecurity
@KeycloakConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig : KeycloakWebSecurityConfigurerAdapter() {


    @Autowired
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        val grantedAuthorityMapper = SimpleAuthorityMapper()
        grantedAuthorityMapper.setPrefix(AUTHORITY_MAPPER_PREFIX)
        grantedAuthorityMapper.setConvertToUpperCase(true)
        val keycloakAuthenticationProvider = keycloakAuthenticationProvider()
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
    override fun httpSessionManager(): HttpSessionManager {
        return HttpSessionManager()
    }

    @Bean
    override fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
        return NullAuthenticatedSessionStrategy()
    }

    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val clientRegistration = ClientRegistration.withRegistrationId("shikimori")
            .clientId("YOUR_CLIENT_ID")
            .clientSecret("YOUR_CLIENT_SECRET")
            .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://shikimori.one/oauth/authorize")
            .tokenUri("https://shikimori.one/oauth/token")
            .userInfoUri("https://shikimori.one/api/users/whoami")
            .userNameAttributeName("id")
            .clientName("Shikimori")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .build()

        return InMemoryClientRegistrationRepository(clientRegistration)
    }

    @Bean
    fun KeycloakConfigResolver(): KeycloakConfigResolver {
        return KeycloakSpringBootConfigResolver()
    }

    override fun keycloakSecurityContextRequestFilter(): KeycloakSecurityContextRequestFilter {
        return KeycloakSecurityContextRequestFilter()
    }


    @Bean
    fun keycloakAuthenticationProcessingFilterRegistrationBean(
        filter: KeycloakAuthenticationProcessingFilter
    ): FilterRegistrationBean<KeycloakAuthenticationProcessingFilter> {
        val registrationBean = FilterRegistrationBean(filter)
        registrationBean.isEnabled = false
        return registrationBean
    }

    @Bean
    fun keycloakPreAuthActionsFilterRegistrationBean(
        filter: KeycloakPreAuthActionsFilter
    ): FilterRegistrationBean<KeycloakPreAuthActionsFilter> {
        val registrationBean = FilterRegistrationBean(filter)
        registrationBean.isEnabled = false
        return registrationBean
    }

    @Bean
    fun keycloakAuthenticatedActionsFilterBean(
        filter: KeycloakAuthenticatedActionsFilter
    ): FilterRegistrationBean<KeycloakAuthenticatedActionsFilter> {
        val registrationBean = FilterRegistrationBean(filter)
        registrationBean.isEnabled = false
        return registrationBean
    }

    @Bean
    fun keycloakSecurityContextRequestFilterBean(
        filter: KeycloakSecurityContextRequestFilter
    ): FilterRegistrationBean<KeycloakSecurityContextRequestFilter> {
        val registrationBean = FilterRegistrationBean(filter)
        registrationBean.isEnabled = false
        return registrationBean
    }

    @Bean
    override fun authenticationEntryPoint(): AuthenticationEntryPoint {
        return KeycloakAuthenticationEntryPoint(adapterDeploymentContext())
    }

    override fun configure(http: HttpSecurity) {
        super.configure(http)
        http.cors()
            .and()
            .csrf()
            .disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .oauth2Login()
            .defaultSuccessUrl("/callback")
            .and()
            .authorizeRequests()
            .antMatchers(
                "/api/anime/**",
                "/api/manga/**",
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
                "/webjars/**"
            )
            .permitAll()
            .antMatchers("/api/users/**")
            .hasAnyRole(USER, ADMIN)
            .anyRequest()
            .authenticated()
    }


    companion object {
        private const val AUTHORITY_MAPPER_PREFIX = "ROLE_"
        private const val ADMIN = "ADMIN"
        private const val USER = "USER"
    }

}
