package com.example.backend.service.image

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.example.backend.jpa.common.Image
import com.example.backend.repository.image.ImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.ImageOutputStream

@Service
class ImageService {

    @Autowired
    private lateinit var imageRepository: ImageRepository

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

    fun saveFileInSThird(filePath: String, data: ByteArray, compress: Boolean = false): String {
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration("https://s3.timeweb.com", "ru-1")
            )
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKeyS3, secretKeyS3))) // <--- заменить
            .build()

        val readyData = if(compress) compressImage(data) else data

        val inputStream = ByteArrayInputStream(readyData)
        val metadata = ObjectMetadata().apply {
            contentLength = readyData.size.toLong()
        }
        s3.putObject(bucketNameS3, filePath, inputStream, metadata)

        return "$domainS3/$filePath"
    }

    fun compressImage(imageBytes: ByteArray): ByteArray {
        val uncompressedImage = ImageIO.read(ByteArrayInputStream(imageBytes))

        // Указываем желаемый формат сжатия
        val compressFormat = determineImageFormat(uncompressedImage)

        val outputStream = ByteArrayOutputStream()

        // Получаем экземпляр ImageWriter для указанного формата
        val imageWriter = ImageIO.getImageWritersByFormatName(compressFormat).next()

        // Получаем параметры сжатия для ImageWriter
        val imageWriteParam = imageWriter.defaultWriteParam
        if (imageWriteParam.canWriteCompressed()) {
            // Указываем желаемое качество сжатия (от 0.0 до 1.0)
            val compressionQuality = 0.3f
            imageWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
            imageWriteParam.compressionQuality = compressionQuality
        }

        // Настраиваем рендеринг для сглаживания
        val renderingHints = RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        renderingHints[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_QUALITY

        // Создаем новый BufferedImage для сжатого изображения
        val compressedImage = BufferedImage(uncompressedImage.width, uncompressedImage.height, BufferedImage.TYPE_INT_RGB)
        val graphics = compressedImage.createGraphics()
        graphics.addRenderingHints(renderingHints)
        graphics.drawImage(uncompressedImage, 0, 0, null)
        graphics.dispose()

        // Записываем сжатое изображение в выходной поток
        val imageOutputStream: ImageOutputStream = ImageIO.createImageOutputStream(outputStream)
        imageWriter.output = imageOutputStream
        imageWriter.write(null, javax.imageio.IIOImage(compressedImage, null, null), imageWriteParam)
        imageOutputStream.close()

        // Получаем сжатые байты из выходного потока
        val compressedBytes = outputStream.toByteArray()
        outputStream.close()

        return compressedBytes
    }

    fun determineImageFormat(image: BufferedImage): String {
        return when (image.type) {
            BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE -> "png"
            BufferedImage.TYPE_INT_RGB -> "jpeg"
            else -> {
                val writerIter = ImageIO.getImageWritersBySuffix("jpeg")
                if (writerIter.hasNext()) {
                    "jpeg"
                } else {
                    "png"
                }
            }
        }
    }



    fun saveFile(file: MultipartFile): String {
        val image = imageRepository.save(
            Image(
                image = file.bytes
            )
        )
        return "$host/images/${image.id}"
    }


    fun save(file: ByteArray): String {
        val image = imageRepository.save(
            Image(
                image = file
            )
        )
        return "$host/images/${image.id}"
    }

    fun getImage(id: String): Optional<Image> {
        return imageRepository.findById(id)
    }
}