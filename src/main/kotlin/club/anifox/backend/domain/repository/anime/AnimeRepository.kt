package club.anifox.backend.domain.repository.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeDefaultFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.character.AnimeCharacterResponse
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.episode.AnimeEpisode
import club.anifox.backend.domain.model.anime.episode.AnimeEpisodeHistory
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.statistics.AnimeStatistics
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import java.time.LocalDate

interface AnimeRepository {
    fun parseTranslations(translationsIDs: List<Int>)

    fun parseAnime()

    fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount>

    fun getAnimeTranslations(): List<AnimeTranslationTable>

    fun getAnimeDetails(token: String?, url: String): AnimeDetail

    fun getAnimeSimilar(page: Int, limit: Int, url: String): List<AnimeLight>

    fun getAnimeRelated(url: String): List<AnimeRelationLight>

    fun getAnimeScreenshots(url: String): List<String>

    fun getAnimeVideos(
        url: String,
        type: AnimeVideoType?,
    ): List<AnimeVideo>

    fun getWeeklySchedule(
        page: Int,
        limit: Int,
        date: LocalDate,
    ): Map<String, List<AnimeLight>>

    fun getAnimeYears(): List<String>

    fun getAnimeStudios(): List<AnimeStudio>

    fun getAnimeGenres(): List<AnimeGenre>

    fun getAnimeEpisodes(
        token: String?,
        url: String,
        page: Int,
        limit: Int,
        sort: AnimeDefaultFilter?,
        translationId: Int?,
        searchQuery: String?,
    ): List<AnimeEpisode>

    fun updateEpisodes(onlyOngoing: Boolean)

    fun addBlocked(
        url: String?,
        shikimoriId: Int?,
    )

    fun getAnime(
        page: Int,
        limit: Int,
        genres: List<String>?,
        status: AnimeStatus?,
        orderBy: AnimeSearchFilter?,
        sort: AnimeDefaultFilter?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studios: List<String>?,
        episodeCount: Int?,
    ): List<AnimeLight>

    fun parseAnimeIntegrations()
    fun getAnimeCharacters(
        page: Int,
        limit: Int,
        url: String,
        role: String?,
        search: String?,
    ): AnimeCharacterResponse

    fun findExternalLinksByAnimeId(animeId: String): List<AnimeExternalLinksTable>
    fun getAnimeEpisodesHistory(url: String, page: Int, limit: Int): List<AnimeEpisodeHistory>
    fun getAnimeStatistics(url: String): AnimeStatistics
}
