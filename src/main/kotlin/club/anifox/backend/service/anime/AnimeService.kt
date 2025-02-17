package club.anifox.backend.service.anime

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
import club.anifox.backend.domain.model.anime.sitemap.AnimeSitemap
import club.anifox.backend.domain.model.anime.statistics.AnimeStatistics
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.domain.repository.anime.AnimeRepository
import club.anifox.backend.jpa.entity.anime.AnimeExternalLinksTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.service.anime.components.block.AnimeBlockComponent
import club.anifox.backend.service.anime.components.common.AnimeCommonComponent
import club.anifox.backend.service.anime.components.episodes.AnimeTranslationsComponent
import club.anifox.backend.service.anime.components.parser.AnimeParseComponent
import club.anifox.backend.service.anime.components.search.AnimeSearchComponent
import club.anifox.backend.service.anime.components.update.AnimeUpdateComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AnimeService : AnimeRepository {
    @Autowired
    private lateinit var animeSearchComponent: AnimeSearchComponent

    @Autowired
    private lateinit var animeCommonComponent: AnimeCommonComponent

    @Autowired
    private lateinit var animeBlockComponent: AnimeBlockComponent

    @Autowired
    private lateinit var animeTranslationsComponent: AnimeTranslationsComponent

    @Autowired
    private lateinit var animeParseComponent: AnimeParseComponent

    @Autowired
    private lateinit var animeUpdateComponent: AnimeUpdateComponent

    override fun getAnime(
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
    ): List<AnimeLight> {
        return animeSearchComponent.getAnimeSearch(
            page = page,
            limit = limit,
            genres = genres,
            status = status,
            orderBy = orderBy,
            sort = sort,
            searchQuery = searchQuery,
            season = season,
            ratingMpa = ratingMpa,
            minimalAge = minimalAge,
            type = type,
            year = year,
            translations = translations,
            studios = studios,
            episodeCount = episodeCount,
        )
    }

    override fun getAnimeDetails(url: String): AnimeDetail {
        return animeCommonComponent.getAnimeByUrl(url)
    }

    override fun getAnimeStatistics(url: String): AnimeStatistics {
        return animeCommonComponent.getAnimeStatistics(url)
    }

    override fun findExternalLinksByAnimeId(animeId: String): List<AnimeExternalLinksTable> {
        return animeCommonComponent.getAnimeExternalLinks(animeId)
    }

    override fun getAnimeCharacters(
        page: Int,
        limit: Int,
        url: String,
        role: String?,
        search: String?,
    ): AnimeCharacterResponse {
        return animeCommonComponent.getAnimeCharactersWithRoles(page, limit, url, role, search)
    }

    override fun getAnimeSimilar(page: Int, limit: Int, url: String): List<AnimeLight> {
        return animeCommonComponent.getAnimeSimilar(page, limit, url)
    }

    override fun getAnimeRelated(url: String): List<AnimeRelationLight> {
        return animeCommonComponent.getAnimeRelated(url)
    }

    override fun getAnimeScreenshots(url: String): List<String> {
        return animeCommonComponent.getAnimeScreenshots(url)
    }

    override fun getAnimeEpisodes(
        token: String?,
        url: String,
        page: Int,
        limit: Int,
        sort: AnimeDefaultFilter?,
        translationId: Int?,
        searchQuery: String?,
    ): List<AnimeEpisode> {
        return animeCommonComponent.getAnimeEpisodes(token, url, page, limit, sort, translationId, searchQuery)
    }

    override fun getAnimeEpisodesHistory(
        url: String,
        page: Int,
        limit: Int,
    ): List<AnimeEpisodeHistory> {
        return animeCommonComponent.getAnimeEpisodesHistory(url, page, limit)
    }

    override fun getAnimeVideos(
        url: String,
        type: AnimeVideoType?,
    ): List<AnimeVideo> {
        return animeCommonComponent.getAnimeVideos(url, type)
    }

    override fun getWeeklySchedule(
        page: Int,
        limit: Int,
        date: LocalDate,
    ): Map<String, List<AnimeLight>> {
        return animeCommonComponent.getWeeklySchedule(page, limit, date)
    }

    override fun getAnimeYears(): List<String> {
        return animeCommonComponent.getAnimeYears()
    }

    override fun getAnimeStudios(): List<AnimeStudio> {
        return animeCommonComponent.getAnimeStudios()
    }

    override fun getAnimeGenres(): List<AnimeGenre> {
        return animeCommonComponent.getAnimeGenres()
    }

    override fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount> {
        return animeTranslationsComponent.getAnimeTranslationsCount(url)
    }

    override fun getAnimeTranslations(): List<AnimeTranslationTable> {
        return animeTranslationsComponent.getAnimeTranslations()
    }

    override fun parseTranslations(translationsIDs: List<Int>) {
        animeTranslationsComponent.addTranslationsToDB(translationsIDs)
    }

    override fun parseAnime() {
        animeParseComponent.addDataToDB()
    }

    override fun parseAnimeIntegrations() {
        animeParseComponent.integrations()
    }

    override fun updateEpisodes(onlyOngoing: Boolean) {
        animeUpdateComponent.update(onlyOngoing = onlyOngoing)
    }

    override fun addBlocked(
        url: String?,
        shikimoriId: Int?,
    ) {
        animeBlockComponent.blockAnime(url, shikimoriId)
    }

    fun getAnimeSitemap(): List<AnimeSitemap> {
        return animeCommonComponent.getAnimeSitemap()
    }
}
