package club.anifox.backend.domain.repository.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeEpisodeFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSortFilter
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable

interface AnimeRepository {
    fun parseTranslations(translationsIDs: List<Int>)
    fun parseAnime()
    fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount>
    fun getAnimeTranslations(): List<AnimeTranslationTable>
    fun getAnimeDetails(url: String): AnimeDetail
    fun getAnimeSimilar(url: String): List<AnimeLight>
    fun getAnimeRelated(url: String): List<AnimeRelationLight>
    fun getAnimeScreenshots(url: String): List<String>
    fun getAnimeVideos(url: String, type: AnimeVideoType?): List<AnimeVideo>
    fun getAnimeYears(): List<String>
    fun getAnimeStudios(): List<AnimeStudio>
    fun getAnimeGenres(): List<AnimeGenre>
    fun getAnimeEpisodes(url: String, page: Int, limit: Int, sort: AnimeEpisodeFilter?): List<AnimeEpisodeLight>
    fun updateEpisodes()
    fun addBlocked(url: String?, shikimoriId: Int?)
    fun getAnime(
        page: Int,
        limit: Int,
        genres: List<String>?,
        status: AnimeStatus?,
        orderBy: AnimeSearchFilter?,
        sort: AnimeSortFilter?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studio: String?,
    ): List<AnimeLight>
}
