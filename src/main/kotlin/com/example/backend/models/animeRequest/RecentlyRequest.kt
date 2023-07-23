@file:UseSerializers(LocalDateTimeSerializer::class)
package com.example.backend.models.animeRequest

import com.example.backend.util.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Serializable
data class RecentlyRequest(
    @SerialName("timingInSeconds")
    val timingInSeconds: Double,
    @SerialName("date")
    val date: LocalDateTime,
    @SerialName("episodeNumber")
    val episodeNumber: Int,
    @SerialName("translationId")
    val translationId: Int,
)