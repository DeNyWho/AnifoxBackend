package club.anifox.backend.domain.model.anime.episode

import java.time.LocalDate

data class AnimeEpisodeHistory(
    val title: String? = "",
    val number: Int,
    val aired: LocalDate? = null,
)
