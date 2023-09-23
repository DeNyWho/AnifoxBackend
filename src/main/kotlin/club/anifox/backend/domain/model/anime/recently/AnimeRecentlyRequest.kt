@file:UseSerializers(LocalDateTimeSerializer::class)

package club.anifox.backend.domain.model.anime.recently

import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Serializable
data class AnimeRecentlyRequest(
    @SerialName("timing_seconds")
    val timingInSeconds: Double,
    @SerialName("date")
    val date: LocalDateTime,
    @SerialName("episode_number")
    val episodeNumber: Int,
    @SerialName("translation_id")
    val translationId: Int,
)
