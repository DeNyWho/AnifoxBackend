package club.anifox.backend.domain.model.anime.translation

import club.anifox.backend.util.serializer.AnimeTranslationCountSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = AnimeTranslationCountSerializer::class)
@Polymorphic
sealed interface AnimeTranslationCount {
    val translation: AnimeTranslation

    @SerialName("count_episodes")
    val countEpisodes: Int
}

@Serializable
@SerialName("default")
data class AnimeTranslationCountDefault(
    override val translation: AnimeTranslation,
    @SerialName("count_episodes")
    override val countEpisodes: Int,
) : AnimeTranslationCount

@Serializable
@SerialName("single")
data class AnimeTranslationCountSingle(
    override val translation: AnimeTranslation,
    @SerialName("count_episodes")
    override val countEpisodes: Int,
    val link: String,
) : AnimeTranslationCount
