package club.anifox.backend.config.jpa

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.criteria.CriteriaBuilder
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.*
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(basePackages = ["club.anifox.backend.jpa.repository"])
@EntityScan(basePackages = ["club.anifox.backend.jpa.entity"])
@EnableTransactionManagement
class JpaConfig(
    @Value("\${spring.datasource.url}") private val url: String,
    @Value("\${spring.datasource.username}") private val username: String,
    @Value("\${spring.datasource.password}") private val password: String,
) {
    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.url = url
        dataSource.username = username
        dataSource.password = password
        return dataSource
    }

    @Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val emf = LocalContainerEntityManagerFactoryBean()
        emf.dataSource = dataSource()
        emf.setPackagesToScan("club.anifox.backend.jpa")
        emf.setJpaProperties(hibernateProperties())
        emf.setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
        return emf
    }

    private fun hibernateProperties(): Properties {
        val properties = Properties()
        properties["hibernate.hbm2ddl.auto"] = "update"
        properties["hibernate.format_sql"] = "true"
        properties["hibernate.jdbc.batch_size"] = "20"
        properties["hibernate.order_inserts"] = "true"
        properties["hibernate.order_updates"] = "true"
        return properties
    }

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    @Bean
    fun getCriteriaBuilder(entityManagerFactory: EntityManagerFactory): CriteriaBuilder {
        return entityManagerFactory.criteriaBuilder
    }
}
