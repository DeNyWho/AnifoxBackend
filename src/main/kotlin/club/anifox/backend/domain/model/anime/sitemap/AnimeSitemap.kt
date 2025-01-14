package club.anifox.backend.domain.model.anime.sitemap

import java.time.LocalDateTime

data class AnimeSitemap(
    val url: String,
    val update: LocalDateTime?,
)
