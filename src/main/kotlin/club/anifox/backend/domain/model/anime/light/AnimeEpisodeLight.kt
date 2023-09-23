package club.anifox.backend.domain.model.anime.light

import club.anifox.backend.domain.model.anime.translation.AnimeEpisodeTranslations
import kotlinx.serialization.Serializable

@Serializable
data class AnimeEpisodeLight(
    val title: String? = "",
    val description: String? = "",
    val number: Int,
    val image: String? = "",
    val translations: List<AnimeEpisodeTranslations> = listOf(),
)
