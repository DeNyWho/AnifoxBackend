package club.anifox.backend.service.anime

import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.AnimeVideoType
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSortFilter
import club.anifox.backend.domain.model.anime.AnimeFranchise
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.AnimeVideo
import club.anifox.backend.domain.model.anime.character.AnimeCharacterResponse
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.episode.AnimeEpisode
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.domain.repository.anime.AnimeRepository
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.service.anime.components.common.AnimeCommonComponent
import club.anifox.backend.service.anime.components.episodes.AnimeTranslationsComponent
import club.anifox.backend.service.anime.components.parser.AnimeParseComponent
import club.anifox.backend.service.anime.components.search.AnimeSearchComponent
import club.anifox.backend.service.anime.components.update.AnimeUpdateComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class AnimeService : AnimeRepository {
    @Autowired
    private lateinit var animeSearchComponent: AnimeSearchComponent

    @Autowired
    private lateinit var animeCommonComponent: AnimeCommonComponent

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
        sort: AnimeSortFilter?,
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

    override fun getAnimeCharacters(
        page: Int,
        limit: Int,
        url: String,
        role: String?,
    ): AnimeCharacterResponse {
        return animeCommonComponent.getAnimeCharactersWithRoles(page, limit, url, role)
    }

    override fun getAnimeSimilar(url: String): List<AnimeLight> {
        return animeCommonComponent.getAnimeSimilar(url)
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
        sort: AnimeSortFilter?,
        translationId: Int?,
        searchQuery: String?,
    ): List<AnimeEpisode> {
        return animeCommonComponent.getAnimeEpisodes(token, url, page, limit, sort, translationId, searchQuery)
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
        date: LocalDate?,
        dayOfWeek: DayOfWeek?,
    ): Map<String, List<AnimeLight>> {
        return animeCommonComponent.getWeeklySchedule(page, limit, date, dayOfWeek)
    }

    override fun getAnimeFranchises(
        url: String,
        type: AnimeRelationFranchise?,
    ): List<AnimeFranchise> {
        return animeCommonComponent.getAnimeFranchise(url, type)
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

    override fun updateEpisodes() {
        animeUpdateComponent.update()
    }

    override fun addBlocked(
        url: String?,
        shikimoriId: Int?,
    ) {
        animeCommonComponent.blockAnime(url, shikimoriId)
    }
}
