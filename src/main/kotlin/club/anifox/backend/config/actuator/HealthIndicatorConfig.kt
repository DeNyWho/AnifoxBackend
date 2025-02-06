package club.anifox.backend.config.actuator

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.annotation.Configuration

@Configuration
class HealthIndicatorConfig : HealthIndicator {
    override fun health(): Health {
        return try {
            Health.up()
                .withDetail("serviceA", "running")
                .withDetail("database", "connected")
                .build()
        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .build()
        }
    }
}
