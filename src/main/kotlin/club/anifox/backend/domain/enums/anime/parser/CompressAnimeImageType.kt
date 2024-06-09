package club.anifox.backend.domain.enums.anime.parser

import club.anifox.backend.domain.enums.image.ImageType

enum class CompressAnimeImageType(val path: String, val imageType: ImageType, private val width: Int, private val height: Int, val compressQuality: Double) {
    Large("large", ImageType.JPG, 400, 640, 0.9),
    Medium("medium", ImageType.JPG, 200, 440, 0.9),
    Cover("cover", ImageType.JPG, 1800, 400, 0.9),
    Screenshot("screenshots", ImageType.JPG, 1920, 1080, 0.9),
    Episodes("episodes", ImageType.JPG, 400, 225, 1.0),
    Avatar("avatar", ImageType.WEBP, 400, 400, 1.0),
    ;

    fun extractWidthAndHeight(): Pair<Int, Int> {
        return Pair(width, height)
    }
}
