package club.anifox.backend.domain.model.anime.statistics

import kotlinx.serialization.Serializable

@Serializable
data class AnimeStatisticsScore(
    val score: Int,
    val votes: Int,
)
