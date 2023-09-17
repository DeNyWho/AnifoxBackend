package club.anifox.backend.service.image

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class ImageService {

    @Value("\${host_url}")
    lateinit var host: String

    @Value("\${access_key_s3}")
    lateinit var accessKeyS3: String

    @Value("\${secret_key_s3}")
    lateinit var secretKeyS3: String

    @Value("\${bucket_name_s3}")
    lateinit var bucketNameS3: String

    @Value("\${domain_s3}")
    lateinit var domainS3: String

    fun saveFileInSThird(filePath: String, data: ByteArray, compress: Boolean = false, width: Int = 0, height: Int = 0): String {
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration("https://s3.timeweb.com", "ru-1"),
            )
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKeyS3, secretKeyS3))) // <--- заменить
            .build()

        val readyData = if (compress) compressImage(imageBytes = data, width, height) else data

        val inputStream = ByteArrayInputStream(readyData)
        val metadata = ObjectMetadata().apply {
            contentLength = readyData.size.toLong()
        }
        s3.putObject(bucketNameS3, filePath, inputStream, metadata)

        return "$domainS3/$filePath"
    }

    private fun compressImage(imageBytes: ByteArray, width: Int, height: Int): ByteArray {
        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(image)
            .outputFormat("png")
            .size(width, height)
            .outputQuality(1.0)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }
}
