package club.anifox.backend.domain.repository.anime

import club.anifox.backend.domain.enums.anime.AnimeOrder
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable

interface AnimeRepository {

    fun getAnime(
        pageNum: Int,
        pageSize: Int,
        genres: List<String>?,
        status: AnimeStatus?,
        order: AnimeOrder?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studio: String?,
    ): List<AnimeLight>

    fun parseTranslations(transltionsIDs: List<Int>)
    fun parseAnime(transltionsIDs: String)
    fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount>
    fun getAnimeTranslations(): List<AnimeTranslationTable>
    fun getAnimeDetails(url: String): AnimeDetail
}
