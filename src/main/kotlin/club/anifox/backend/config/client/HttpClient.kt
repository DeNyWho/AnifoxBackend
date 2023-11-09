package club.anifox.backend.config.client

import club.anifox.backend.domain.constants.Constants
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpClient {

    val domainMutex = Mutex()

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
            install("RateLimiter") {
                requestPipeline.intercept(HttpRequestPipeline.Before) {
                    val allowedHosts = listOf(Constants.JIKAN, Constants.KITSU, Constants.HAGLUND, Constants.SHIKIMORI)
                    val originalHost = context.url.host
                    if (allowedHosts.contains(originalHost)) {
                        domainMutex.lock()
                        delay(1000)
                        try {
                            this.proceed()
                        } finally {
                            domainMutex.unlock()
                        }
                    }
                }
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
}
