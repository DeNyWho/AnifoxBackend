package club.anifox.backend.config.client

import club.anifox.backend.domain.constants.Constants
import io.ktor.client.*
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
class Network {
    val domainMutex = Mutex()

    @Bean
    fun httpClient(): HttpClient {
        return HttpClient(Java) {
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
            install("RateLimiter") {
                requestPipeline.intercept(HttpRequestPipeline.Before) {
                    if (listOf(Constants.EDGE).contains(context.url.host)) {
                        domainMutex.lock()
                        delay(2000)
                        try {
                            this.proceed()
                        } finally {
                            domainMutex.unlock()
                        }
                    }
                    if (listOf(Constants.SHIKIMORI).contains(context.url.host)) {
                        domainMutex.lock()
                        delay(3000)
                        try {
                            this.proceed()
                        } finally {
                            domainMutex.unlock()
                        }
                    }
                    if (listOf(Constants.JIKAN, Constants.KITSU, Constants.EDGE).contains(context.url.host)) {
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

            install(HttpRequestRetry) {
                maxRetries = 5
                retryIf { request, response ->
                    request.url.host == Constants.JIKAN && response.status == HttpStatusCode.InternalServerError
                }
                delayMillis { retry ->
                    retry * 30000L
                } // retries in 3, 6, 9, etc. seconds
            }
        }
    }
}
