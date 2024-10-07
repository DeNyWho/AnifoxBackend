@file:UseSerializers(LocalDateTimeSerializer::class)

package club.anifox.backend.domain.model.anime.episode

import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class AnimeEpisodeProgressRequest(
    @SerialName("timing_seconds")
    val timingInSeconds: Double,
    @SerialName("translation_id")
    val translationId: Int,
)
