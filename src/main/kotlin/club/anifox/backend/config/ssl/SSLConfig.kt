package club.anifox.backend.config.ssl

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.ssl.SSLContexts
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.authorization.client.AuthzClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext

@Configuration
class SSLConfig(
    @Value("\${keycloak.auth-server-url}") private val authUrl: String,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.resource}") private val clientId: String,
    @Value("\${keycloak.credentials.secret}") private val secret: String,
    @Value("\${keycloak-custom.admin-user}") private val adminUser: String,
    @Value("\${keycloak-custom.admin-password}") private val adminPass: String,
    @Value("\${server.ssl.key-store}") private val keyStorePath: String,
    @Value("\${server.ssl.key-store-password}") private val keyStorePassword: String,
    @Value("\${server.ssl.trust-store}") private val trustStorePath: String,
    @Value("\${server.ssl.trust-store-password}") private val trustStorePassword: String,
) {

    @Bean
    fun sslContext(): SSLContext {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(FileInputStream(keyStorePath), keyStorePassword.toCharArray())
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(FileInputStream(trustStorePath), trustStorePassword.toCharArray())

        return SSLContexts.custom()
            .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
            .loadTrustMaterial(trustStore, null)
            .build()
    }

    @Bean
    fun httpClient(): HttpClient {
        return HttpClient(Java) {
            engine {
                config {
//                    sslContext(sslContext())
                }
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    },
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }

    @Bean
    fun authzClient(): AuthzClient {
        val clientCredentials: MutableMap<String, Any?> = HashMap()
        clientCredentials["secret"] = secret

        val builder = HttpClientBuilder.create()
        builder.setSSLContext(sslContext())

        val configuration = org.keycloak.authorization.client.Configuration(
            authUrl,
            realm,
            clientId,
            clientCredentials,
            builder.build(),
        )

        return AuthzClient.create(configuration)
    }

    @Bean
    fun keycloak(): Keycloak {
        val config = KeycloakBuilder.builder()
            .serverUrl(authUrl)
            .realm(realm)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(clientId)
            .clientSecret(secret)
            .username(adminUser)
            .password(adminPass)
            .resteasyClient(
                ResteasyClientBuilderImpl()
                    .connectionPoolSize(12)
                    .keyStore(
                        KeyStore.getInstance(KeyStore.getDefaultType())
                            .apply {
                                FileInputStream(keyStorePath).use {
                                    load(it, keyStorePassword.toCharArray())
                                }
                            },
                        keyStorePassword.toCharArray(),
                    )
                    .build(),
            )
            .build()
        return config
    }
}
