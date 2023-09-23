package club.anifox.backend.domain.model.anime.light

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeImagesTypes
import club.anifox.backend.domain.model.anime.AnimeStudio
import kotlinx.serialization.Serializable

@Serializable
data class AnimeLight(
    var title: String = "",
    var image: AnimeImagesTypes = AnimeImagesTypes(),
    val url: String = "",
    var type: AnimeType = AnimeType.Tv,
    val rating: Double? = 0.0,
    val ratingMpa: String = "",
    val minimalAge: Int = 0,
    val year: Int = 0,
    val status: AnimeStatus = AnimeStatus.Ongoing,
    val season: AnimeSeason = AnimeSeason.Summer,
    val episodes: Int = 0,
    val episodesAired: Int? = 0,
    val accentColor: String = "",
    val studio: List<AnimeStudio> = listOf(),
    val genres: List<AnimeGenre> = listOf(),
)
