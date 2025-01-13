package club.anifox.backend.config.jpa

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Configuration
class TransactionConfig(private val transactionManager: PlatformTransactionManager) {

    @Bean
    fun transactionTemplate(): TransactionTemplate {
        return TransactionTemplate(transactionManager)
    }
}
