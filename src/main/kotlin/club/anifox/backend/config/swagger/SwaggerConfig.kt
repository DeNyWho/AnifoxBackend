package club.anifox.backend.config.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
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
                            .name("Akhunov Denis"),
                    )
                    .description("AniFox API")
                    .version("v2.1.0"),
            )
    }
}
