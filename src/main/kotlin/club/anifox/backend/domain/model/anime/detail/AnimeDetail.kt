@file:UseSerializers(LocalDateTimeSerializer::class, LocalDateSerializer::class)

package club.anifox.backend.domain.model.anime.detail

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeImages
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeUserStats
import club.anifox.backend.domain.model.anime.translation.AnimeTranslation
import club.anifox.backend.util.serializer.AnimeDetailSerializer
import club.anifox.backend.util.serializer.LocalDateSerializer
import club.anifox.backend.util.serializer.LocalDateTimeSerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable(with = AnimeDetailSerializer::class)
@Polymorphic
@SerialName("without_type")
sealed interface AnimeDetail {
    var title: String
    var image: AnimeImages

    @SerialName("player_link")
    val playerLink: String?
    val url: String

    val type: AnimeType

    @SerialName("rating_mpa")
    val ratingMpa: String

    @SerialName("minimal_age")
    val minimalAge: Int
    val rating: Double?

    @SerialName("shikimori_rating")
    val shikimoriRating: Double
    val year: Int
    val status: AnimeStatus
    val season: AnimeSeason

    @SerialName("episodes")
    val episodes: Int?

    @SerialName("episodes_aired")
    val episodesAired: Int?

    @SerialName("next_episode_on")
    val nextEpisode: LocalDateTime?

    @SerialName("released_on")
    val releasedOn: LocalDate?

    @SerialName("aired_on")
    val airedOn: LocalDate
    val description: String?

    @SerialName("other_title")
    val titleOther: List<String>?

    @SerialName("english")
    val titleEnglish: List<String>?

    @SerialName("japanese")
    val titleJapan: List<String>?
    val synonyms: List<String>?
    val studio: List<AnimeStudio>
    val genres: List<AnimeGenre>
    val translations: List<AnimeTranslation>?
}

@Serializable
@SerialName("default")
data class AnimeDetailDefault(
    override var title: String = "",
    override var image: AnimeImages = AnimeImages(),
    @SerialName("player_link")
    override val playerLink: String? = "",
    override val url: String = "",
    override val type: AnimeType,
    @SerialName("rating_mpa")
    override val ratingMpa: String = "",
    @SerialName("minimal_age")
    override val minimalAge: Int = 0,
    override val rating: Double? = 0.0,
    @SerialName("shikimori_rating")
    override val shikimoriRating: Double = 0.0,
    override val year: Int = 0,
    override val status: AnimeStatus,
    override val season: AnimeSeason,
    @SerialName("episodes")
    override val episodes: Int? = 0,
    @SerialName("episodes_aired")
    override val episodesAired: Int? = 0,
    @SerialName("next_episode_on")
    override val nextEpisode: LocalDateTime? = LocalDateTime.now(),
    @SerialName("released_on")
    override val releasedOn: LocalDate? = LocalDate.now(),
    @SerialName("aired_on")
    override val airedOn: LocalDate = LocalDate.now(),
    override val description: String? = "",
    @SerialName("other_title")
    override val titleOther: List<String>? = listOf(),
    @SerialName("english")
    override val titleEnglish: List<String>? = listOf(),
    @SerialName("japanese")
    override val titleJapan: List<String>? = listOf(),
    override val synonyms: List<String>? = listOf(),
    override val studio: List<AnimeStudio> = listOf(),
    override val genres: List<AnimeGenre> = listOf(),
    override val translations: List<AnimeTranslation>? = listOf(),
) : AnimeDetail

@Serializable
@SerialName("with_user")
data class AnimeDetailWithUser
@OptIn(ExperimentalSerializationApi::class)
constructor(
    override var title: String = "",
    override var image: AnimeImages = AnimeImages(),
    @SerialName("player_link")
    override val playerLink: String? = "",
    override val url: String = "",
    override val type: AnimeType,
    @SerialName("rating_mpa")
    override val ratingMpa: String = "",
    @SerialName("minimal_age")
    override val minimalAge: Int = 0,
    override val rating: Double? = 0.0,
    @SerialName("shikimori_rating")
    override val shikimoriRating: Double = 0.0,
    override val year: Int = 0,
    override val status: AnimeStatus,
    override val season: AnimeSeason,
    @SerialName("episodes")
    override val episodes: Int? = 0,
    @SerialName("episodes_aired")
    override val episodesAired: Int? = 0,
    @SerialName("next_episode_on")
    override val nextEpisode: LocalDateTime? = LocalDateTime.now(),
    @SerialName("released_on")
    override val releasedOn: LocalDate? = LocalDate.now(),
    @SerialName("aired_on")
    override val airedOn: LocalDate = LocalDate.now(),
    override val description: String? = "",
    @SerialName("other_title")
    override val titleOther: List<String>? = listOf(),
    @SerialName("english")
    override val titleEnglish: List<String>? = listOf(),
    @SerialName("japanese")
    override val titleJapan: List<String>? = listOf(),
    override val synonyms: List<String>? = listOf(),
    override val studio: List<AnimeStudio> = listOf(),
    override val genres: List<AnimeGenre> = listOf(),
    override val translations: List<AnimeTranslation>? = listOf(),
    @EncodeDefault
    val user: AnimeUserStats? = null,
) : AnimeDetail

// Extension function для конвертации
fun AnimeDetailDefault.withUser(userStats: AnimeUserStats = AnimeUserStats()) =
    AnimeDetailWithUser(
        title = this.title,
        image = this.image,
        playerLink = this.playerLink,
        url = this.url,
        type = this.type,
        ratingMpa = this.ratingMpa,
        minimalAge = this.minimalAge,
        rating = this.rating,
        shikimoriRating = this.shikimoriRating,
        year = this.year,
        status = this.status,
        season = this.season,
        episodes = this.episodes,
        episodesAired = this.episodesAired,
        nextEpisode = this.nextEpisode,
        releasedOn = this.releasedOn,
        airedOn = this.airedOn,
        description = this.description,
        titleOther = this.titleOther,
        titleEnglish = this.titleEnglish,
        titleJapan = this.titleJapan,
        synonyms = this.synonyms,
        studio = this.studio,
        genres = this.genres,
        translations = this.translations,
        user = userStats,
    )
