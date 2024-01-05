package club.anifox.backend.config.swagger

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(servers = [Server(url = "/")])
class SwaggerConfig {

    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("aniFox")
            .pathsToMatch("/**")
            .build()
    }

    @Bean
    fun aniFoxOpenApi(): OpenAPI? {
        return OpenAPI()
            .info(
                Info()
                    .title("AniFox Api")
                    .contact(
                        Contact()
                            .email("denis.akhunov123@gmail.com")
                            .name("Akhunov Denis")
                            .url("https://github.com/DeNyWho"),
                    )
                    .description("AniFox API")
                    .version("v3.0.1"),
            )
    }
}
