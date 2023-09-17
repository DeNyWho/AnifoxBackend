package club.anifox.backend.domain.model.anime

import java.awt.image.BufferedImage

data class AnimeBufferedImages(
    val large: BufferedImage? = null,
    val medium: BufferedImage? = null,
)
