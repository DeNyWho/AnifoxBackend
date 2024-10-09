package club.anifox.backend.domain.model.anime.translation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeTranslationCount(
    val translation: AnimeTranslation = AnimeTranslation(),
    @SerialName("count_episodes")
    val countEpisodes: Int = 0,
)
