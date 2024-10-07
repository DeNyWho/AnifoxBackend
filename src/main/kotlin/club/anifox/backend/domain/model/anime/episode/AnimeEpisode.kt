package club.anifox.backend.domain.model.anime.episode

import club.anifox.backend.domain.model.anime.translation.AnimeEpisodeTranslations

open class AnimeEpisode(
    val title: String? = "",
    val description: String? = "",
    val number: Int,
    val image: String? = "",
    val translations: List<AnimeEpisodeTranslations> = listOf(),
)

class AnimeEpisodeLight(
    title: String? = "",
    description: String? = "",
    number: Int,
    image: String? = "",
    translations: List<AnimeEpisodeTranslations> = listOf(),
) : AnimeEpisode(title, description, number, image, translations)

class AnimeEpisodeUser(
    title: String? = "",
    description: String? = "",
    number: Int,
    image: String? = "",
    translations: List<AnimeEpisodeTranslations> = listOf(),
    val timing: Double,
) : AnimeEpisode(title, description, number, image, translations)
