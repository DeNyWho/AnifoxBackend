@file:UseSerializers(LocalDateTimeSerializer::class)

package club.anifox.backend.domain.dto.anime.kodik

import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Serializable
data class KodikAnimeDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("title_orig")
    val title: String = "",
    @SerialName("link")
    val link: String = "",
    @SerialName("translation")
    val translation: KodikTranslationDto = KodikTranslationDto(),
    @SerialName("last_episode")
    val lastEpisode: Int = 0,
    @SerialName("episodes_count")
    val episodesCount: Int = 0,
    @SerialName("shikimori_id")
    val shikimoriId: String = "",
    @SerialName("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @SerialName("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    @SerialName("seasons")
    val seasons: Map<String, KodikSeasonDto> = mapOf(),
    @SerialName("material_data")
    var materialData: KodikMaterialDataDto = KodikMaterialDataDto(),
    @SerialName("screenshots")
    val screenshots: List<String> = listOf(),
)
