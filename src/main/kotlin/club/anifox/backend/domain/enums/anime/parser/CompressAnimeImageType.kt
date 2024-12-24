package club.anifox.backend.domain.enums.anime.parser

import club.anifox.backend.domain.enums.image.ImageType

enum class CompressAnimeImageType(val path: String, val imageType: ImageType, private val width: Int, private val height: Int, val compressQuality: Double) {
    LargeJikan("large", ImageType.JPG, 425, 600, 1.0),
    MediumJikan("medium", ImageType.JPG, 390, 554, 1.0),
    LargeKitsu("large", ImageType.JPG, 550, 780, 1.0),
    MediumKitsu("medium", ImageType.JPG, 390, 554, 1.0),
    Cover("cover", ImageType.JPG, 3360, 800, 0.9),
    Screenshot("screenshots", ImageType.JPG, 1920, 1080, 0.9),
    Episodes("episodes", ImageType.JPG, 400, 225, 1.0),
    Avatar("avatar", ImageType.WEBP, 400, 400, 1.0),
    CharacterImage("character", ImageType.JPG, 400, 400, 1.0),
    ;

    fun extractWidthAndHeight(): Pair<Int, Int> {
        return Pair(width, height)
    }
}
