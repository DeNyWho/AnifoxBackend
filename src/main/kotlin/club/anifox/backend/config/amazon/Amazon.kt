package club.anifox.backend.config.amazon

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Amazon {
    @Value("\${access_key_s3}")
    lateinit var accessKeyS3: String

    @Value("\${secret_key_s3}")
    lateinit var secretKeyS3: String

    @Bean
    fun amazonS3(): AmazonS3 {
        return AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration("https://s3.timeweb.cloud", "ru-1"),
            )
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKeyS3, secretKeyS3)))
            .build()
    }
}
