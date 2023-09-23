package club.anifox.backend.domain.model.anime.translation

import kotlinx.serialization.Serializable

@Serializable
data class AnimeTranslation(
    val id: Int = 0,
    val title: String = "",
    val voice: String = "",
)
