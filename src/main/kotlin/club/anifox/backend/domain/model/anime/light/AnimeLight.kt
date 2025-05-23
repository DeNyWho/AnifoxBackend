package club.anifox.backend.domain.model.anime.light

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeImages
import club.anifox.backend.domain.model.anime.AnimeStudio
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeLight(
    var title: String = "",
    var image: AnimeImages = AnimeImages(),
    val url: String = "",
    val type: AnimeType,
    val rating: Double? = 0.0,
    @SerialName("rating_mpa")
    val ratingMpa: String = "",
    @SerialName("minimal_age")
    val minimalAge: Int = 0,
    val description: String,
    val year: Int = 0,
    val status: AnimeStatus,
    val season: AnimeSeason,
    val episodes: Int? = null,
    @SerialName("episodes_aired")
    val episodesAired: Int? = null,
    @SerialName("accent_color")
    val accentColor: String = "",
    val studio: List<AnimeStudio> = listOf(),
    val genres: List<AnimeGenre> = listOf(),
)
