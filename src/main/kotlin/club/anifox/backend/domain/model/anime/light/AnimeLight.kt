package club.anifox.backend.domain.model.anime.light

import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeImagesTypes
import club.anifox.backend.domain.model.anime.AnimeStudio
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable

@Serializable
@JsonInclude(JsonInclude.Include.ALWAYS)
data class AnimeLight(
    val url: String = "",
    var title: String = "",
    var image: AnimeImagesTypes = AnimeImagesTypes(),
    var type: String = "",
    val rating: Double? = null,
    val studio: List<AnimeStudio> = listOf(),
    val season: String = "",
    val year: Int = 0,
    val episodesCount: Int = 0,
    val genres: List<AnimeGenre> = listOf(),
    val status: String = "",
    val ratingMpa: String = "",
    val minimalAge: Int = 0,
    val accentColor: String = "",
)
