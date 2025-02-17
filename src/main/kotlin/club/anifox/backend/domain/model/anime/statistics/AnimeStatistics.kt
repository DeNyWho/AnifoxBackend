package club.anifox.backend.domain.model.anime.statistics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeStatistics(
    val watching: Int,
    val completed: Int,
    @SerialName("on_hold")
    val onHold: Int,
    val dropped: Int,
    @SerialName("plan_to_watch")
    val planToWatch: Int,
    val total: Int,
    val scores: List<AnimeStatisticsScore>,
)
