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
    @SerialName("total_status")
    val totalStatus: Int,
    @SerialName("total_votes")
    val totalVotes: Int,
    val scores: List<AnimeStatisticsScore>,
)
