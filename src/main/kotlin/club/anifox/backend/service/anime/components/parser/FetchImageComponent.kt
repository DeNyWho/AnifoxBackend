package club.anifox.backend.service.anime.components.parser

import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.domain.model.anime.AnimeImages
import club.anifox.backend.service.anime.components.jikan.JikanComponent
import club.anifox.backend.service.anime.components.kitsu.KitsuComponent
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

@Component
class FetchImageComponent {

    @Autowired
    private lateinit var jikanComponent: JikanComponent

    @Autowired
    private lateinit var kitsuComponent: KitsuComponent

    @Autowired
    private lateinit var imageService: ImageService

    suspend fun fetchAndSaveAnimeImages(shikimoriId: Int, kitsuId: Int?, urlLinking: String): Pair<AnimeImages, BufferedImage>? {
        val kitsuImages = if (kitsuId != null) {
            fetchKitsuImages(kitsuId)
        } else {
            null
        }
        val jikanImages = fetchJikanImages(shikimoriId)

        return runCatching {
            when {
                kitsuImages != null -> saveKitsuImages(kitsuImages, urlLinking)
                else -> saveJikanImages(jikanImages, urlLinking)
            }
        }.getOrNull()
    }

    private suspend fun fetchKitsuImages(kitsuId: Int): AnimeImages? {
        val kitsuData = kitsuComponent.fetchKitsuAnime(kitsuId).data
        return kitsuData?.let {
            AnimeImages(
                large = it.attributesKitsu.posterImage.large ?: "",
                medium = it.attributesKitsu.posterImage.original ?: "",
                cover = it.attributesKitsu.coverImage.coverLarge,
            )
        }
    }

    private suspend fun fetchJikanImages(shikimoriId: Int): AnimeImages {
        val jikanData = jikanComponent.fetchJikan(shikimoriId).data
        return jikanData.images.jikanJpg.let {
            AnimeImages(
                large = it.largeImageUrl,
                medium = it.mediumImageUrl,
                cover = null,
            )
        }
    }

    private suspend fun saveKitsuImages(images: AnimeImages, urlLinking: String): Pair<AnimeImages, BufferedImage> {
        val (large, medium, cover) = images.extractUrlsWithCover()

        val finalImages = AnimeImages(
            large = saveImage(large, CompressAnimeImageType.Large, urlLinking),
            medium = saveImage(medium, CompressAnimeImageType.Medium, urlLinking),
            cover = saveImage(cover ?: "", CompressAnimeImageType.Cover, urlLinking),
        )

        return Pair(
            finalImages,
            withContext(Dispatchers.IO) {
                ImageIO.read(URL(finalImages.large))
            },
        )
    }

    private suspend fun saveJikanImages(images: AnimeImages, urlLinking: String): Pair<AnimeImages, BufferedImage> {
        val (large, medium) = images.extractUrls()

        val finalImages = AnimeImages(
            large = saveImage(large, CompressAnimeImageType.Large, urlLinking),
            medium = saveImage(medium, CompressAnimeImageType.Medium, urlLinking),
        )

        return Pair(
            finalImages,
            withContext(Dispatchers.IO) {
                ImageIO.read(URL(finalImages.large))
            },
        )
    }

    suspend fun saveImage(url: String, type: CompressAnimeImageType, urlLinking: String, compress: Boolean = true): String {
        return withContext(Dispatchers.IO) {
            val bytes = URL(url).readBytes()
            val fileName = "${mdFive(UUID.randomUUID().toString())}.${type.imageType.textFormat()}"
            val path = "images/anime/${type.path}/$urlLinking/$fileName"

            val extractedWidthAndHeight = type.extractWidthAndHeight()

            imageService.saveFileInSThird(
                filePath = path,
                data = bytes,
                type = type,
                compress = compress,
                width = extractedWidthAndHeight.first,
                height = extractedWidthAndHeight.second,
            )
        }
    }

    private fun AnimeImages.extractUrlsWithCover(): Triple<String, String, String?> {
        return Triple(large, medium, cover)
    }

    private fun AnimeImages.extractUrls(): Pair<String, String> {
        return Pair(large, medium)
    }
}
