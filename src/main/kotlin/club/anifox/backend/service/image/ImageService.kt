package club.anifox.backend.service.image

import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class ImageService {

    @Value("\${bucket_name_s3}")
    lateinit var bucketNameS3: String

    @Value("\${domain_s3}")
    lateinit var domainS3: String

    @Autowired
    private lateinit var amazonS3: AmazonS3

    fun deleteObjectsInFolder(folderPath: String) {
        for (file in amazonS3.listObjects(bucketNameS3, folderPath).objectSummaries) {
            amazonS3.deleteObject(bucketNameS3, file.key)
        }
    }

    suspend fun saveFileInSThird(filePath: String, data: ByteArray, compress: Boolean = false, width: Int = 0, height: Int = 0, newImage: Boolean = false, type: CompressAnimeImageType): String = withContext(Dispatchers.IO) {
        val readyData = if (compress) compressImage(data, type, width, height) else data
        val inputStream = ByteArrayInputStream(readyData)
        val metadata = ObjectMetadata().apply {
            contentLength = readyData.size.toLong()
        }
        if (newImage) {
            amazonS3.deleteObject(bucketNameS3, filePath)
        }
        var uploaded = false
        var times = 3
        while (!uploaded && times > 0) {
            delay(500)
            uploaded = try {
                amazonS3.putObject(bucketNameS3, filePath, inputStream, metadata)
                true
            } catch (e: Exception) {
                times -= 1
                false
            }
        }
        return@withContext "$domainS3/$filePath"
    }

    private fun compressImage(imageBytes: ByteArray, type: CompressAnimeImageType, width: Int, height: Int): ByteArray {
        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(image)
            .outputFormat(type.imageType.textFormat())
            .size(width, height)
            .outputQuality(type.compressQuality)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }
}
