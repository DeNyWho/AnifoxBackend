package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.Nullable

@Serializable
data class ShikimoriDto(
    @SerialName("id")
    val id: Int,
    @SerialName("kind")
    val kind: String,
    @SerialName("status")
    val status: String = "",
    @SerialName("score")
    val score: String = "",
    @SerialName("russian")
    val russian: String = "",
    @SerialName("license_name_ru")
    val russianLic: String? = null,
    @SerialName("description")
    val description: String = "",
    @Nullable
    @SerialName("english")
    val english: List<String?> = listOf(),
    @Nullable
    @SerialName("synonyms")
    val synonyms: List<String> = listOf(),
    @Nullable
    @SerialName("rating")
    val rating: String,
    @Nullable
    @SerialName("japanese")
    val japanese: List<String> = listOf(),
    @SerialName("genres")
    val genres: List<ShikimoriGenresDto> = listOf(),
    @SerialName("studios")
    val studios: List<ShikimoriStudiosDto> = listOf(),
    @SerialName("videos")
    val videos: List<ShikimoriVideoDto> = listOf(),
    @SerialName("fandubbers")
    val fandubbers: List<String> = listOf(),
    @SerialName("episodes")
    val episodes: Int = 0,
    @SerialName("episodes_aired")
    val episodesAired: Int = 0,
    @SerialName("aired_on")
    val airedOn: String = "",
    @SerialName("released_on")
    val releasedOn: String? = null,
    @SerialName("next_episode_at")
    val nextEpisodeAt: String? = null,
    @SerialName("rates_statuses_stats")
    val usersRatesStats: List<ShikimoriFavoriteListDto> = listOf(),
)
