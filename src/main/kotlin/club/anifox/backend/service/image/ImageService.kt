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

    suspend fun saveFileInSThird(filePath: String, data: ByteArray, compress: Boolean = false, newImage: Boolean = false, type: CompressAnimeImageType): String = withContext(Dispatchers.IO) {
        when (type) {
            CompressAnimeImageType.LargeKitsu, CompressAnimeImageType.LargeJikan, CompressAnimeImageType.MediumKitsu, CompressAnimeImageType.MediumJikan, CompressAnimeImageType.Cover -> {
                val folderPath = filePath.substringBeforeLast("/")
                val existingObjects = amazonS3.listObjects(bucketNameS3, folderPath)
                if (existingObjects.objectSummaries.isNotEmpty()) {
                    return@withContext "$domainS3/${existingObjects.objectSummaries.first().key}"
                }
            }
            CompressAnimeImageType.Episodes -> { }
            CompressAnimeImageType.Screenshot -> { }
            CompressAnimeImageType.Avatar -> { }
        }

        val readyData = if (compress) compressImage(data, type) else data
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

    private fun compressImage(imageBytes: ByteArray, type: CompressAnimeImageType): ByteArray {
        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
        val outputStream = ByteArrayOutputStream()
        val size = type.extractWidthAndHeight()

        Thumbnails.of(image)
            .outputFormat(type.imageType.textFormat())
            .size(size.first, size.second)
            .outputQuality(type.compressQuality)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }
}
