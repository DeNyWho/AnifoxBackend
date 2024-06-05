package club.anifox.backend.domain.enums.anime.parser

import club.anifox.backend.domain.enums.image.ImageType

enum class CompressAnimeImageType(val path: String, val imageType: ImageType, private val width: Int, private val height: Int, val compressQuality: Double) {
    Large("large", ImageType.PNG, 400, 640, 1.0),
    Medium("medium", ImageType.JPG, 200, 440, 1.0),
    Cover("cover", ImageType.PNG, 800, 200, 1.0),
    Screenshot("screenshots", ImageType.JPG, 1920, 1080, 0.9),
    Episodes("episodes", ImageType.JPG, 400, 225, 1.0),
    Avatar("avatar", ImageType.WEBP, 400, 400, 1.0),
    ;

    fun extractWidthAndHeight(): Pair<Int, Int> {
        return Pair(width, height)
    }
}
