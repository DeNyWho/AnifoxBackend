package club.anifox.backend.domain.model.anime.translation

data class AnimeTranslationCount(
    val translation: AnimeTranslation = AnimeTranslation(),
    val countEpisodes: Int = 0,
)
