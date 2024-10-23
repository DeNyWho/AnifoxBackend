@file:UseSerializers(LocalDateTimeSerializer::class, LocalDateSerializer::class)

package club.anifox.backend.domain.model.anime.detail

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeImages
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.translation.AnimeTranslation
import club.anifox.backend.util.serializer.LocalDateSerializer
import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class AnimeDetail(
    var title: String = "",
    var image: AnimeImages = AnimeImages(),
    @SerialName("player_link")
    val playerLink: String? = null,
    val url: String = "",
    val type: AnimeType? = null,
    @SerialName("rating_mpa")
    val ratingMpa: String = "",
    @SerialName("minimal_age")
    val minimalAge: Int = 0,
    val rating: Double? = null,
    @SerialName("shikimori_rating")
    val shikimoriRating: Double = 0.0,
    val year: Int = 0,
    val status: AnimeStatus = AnimeStatus.Ongoing,
    val season: AnimeSeason = AnimeSeason.Summer,
    @SerialName("episodes")
    val episodes: Int? = null,
    @SerialName("episodes_aired")
    val episodesAired: Int? = null,
    @SerialName("next_episode_on")
    val nextEpisode: LocalDateTime? = null,
    @SerialName("released_on")
    val releasedOn: LocalDate? = null,
    @SerialName("aired_on")
    val airedOn: LocalDate = LocalDate.now(),
    val description: String? = null,
    @SerialName("other_title")
    val titleOther: List<String>? = null,
    @SerialName("english")
    val titleEnglish: List<String>? = null,
    @SerialName("japanese")
    val titleJapan: List<String>? = null,
    val synonyms: List<String> = listOf(),
    val studio: List<AnimeStudio> = listOf(),
    val genres: List<AnimeGenre> = listOf(),
    val translations: List<AnimeTranslation>? = null,
)
