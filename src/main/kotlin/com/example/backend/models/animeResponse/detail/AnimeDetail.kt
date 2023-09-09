//@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)
package com.example.backend.models.animeResponse.detail

import com.example.backend.jpa.anime.AnimeGenreTable
import com.example.backend.jpa.anime.AnimeStudiosTable
import com.example.backend.jpa.anime.AnimeTranslationTable
import com.example.backend.models.anime.AnimeImagesTypes
import com.example.backend.models.animeParser.kodik.AnimeTranslations
import com.example.backend.models.users.StatusFavourite
import com.example.backend.util.LocalDateSerializer
import com.example.backend.util.LocalDateTimeSerializer
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalDateTime


//@Serializable
@JsonInclude(JsonInclude.Include.ALWAYS)
data class AnimeDetail(
    val url: String? = null,
    var title: String? = null,
    var image: AnimeImagesTypes = AnimeImagesTypes(),
    val studio: List<AnimeStudiosTable> = listOf(),
    val season: String? = null,
    val description: String? = null,
    val otherTitles: List<String> = listOf(),
    val rating: Double? = null,
    val nextEpisode: LocalDateTime? = null,
    val year: Int? = 0,
    val linkPlayer: String = "",
    val shikimoriRating: Double = 0.0,
    val releasedAt: LocalDate = LocalDate.now(),
    val airedAt: LocalDate = LocalDate.now(),
    val type: String? = null,
    val episodesCount: Int? = null,
    val episodesCountAired: Int? = null,
    val genres: List<AnimeGenreTable> = listOf(),
    val status: String? = null,
    val ratingMpa: String? = null,
    val minimalAge: Int? = null,
    val translations: List<AnimeTranslationTable> = listOf(),
)