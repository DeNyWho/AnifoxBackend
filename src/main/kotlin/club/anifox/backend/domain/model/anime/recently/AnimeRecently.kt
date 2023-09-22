@file:UseSerializers(LocalDateTimeSerializer::class)

package club.anifox.backend.domain.model.anime.recently

import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Serializable
data class AnimeRecently(
    val anime: AnimeLight,
    val date: LocalDateTime,
    val timingInSeconds: Double,
    val episode: AnimeEpisodeLight,
    val translationId: Int,
)
