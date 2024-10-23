package club.anifox.backend.domain.model.anime.episode

import club.anifox.backend.domain.model.anime.translation.AnimeEpisodeTranslations
import java.time.LocalDate

open class AnimeEpisode(
    val title: String? = "",
    val description: String? = "",
    val number: Int,
    val image: String? = "",
    val aired: LocalDate? = null,
    var filler: Boolean = false,
    var recap: Boolean = false,
    val translations: List<AnimeEpisodeTranslations> = listOf(),
)

class AnimeEpisodeLight(
    title: String? = "",
    description: String? = "",
    number: Int,
    image: String? = "",
    aired: LocalDate? = null,
    filler: Boolean = false,
    recap: Boolean = false,
    translations: List<AnimeEpisodeTranslations> = listOf(),
) : AnimeEpisode(title, description, number, image, aired, filler, recap, translations)

class AnimeEpisodeUser(
    title: String? = "",
    description: String? = "",
    number: Int,
    image: String? = "",
    aired: LocalDate? = null,
    filler: Boolean = false,
    recap: Boolean = false,
    translations: List<AnimeEpisodeTranslations> = listOf(),
    val timing: Double,
) : AnimeEpisode(title, description, number, image, aired, filler, recap, translations)
