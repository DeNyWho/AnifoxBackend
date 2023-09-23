package club.anifox.backend.domain.model.anime.translation

import kotlinx.serialization.Serializable

@Serializable
data class AnimeEpisodeTranslations(
    val id: Int,
    val link: String,
    val title: String,
    val type: String,
)
