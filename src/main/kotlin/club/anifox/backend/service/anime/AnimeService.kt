package club.anifox.backend.service.anime

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.filter.AnimeEpisodeFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.model.anime.AnimeGenre
import club.anifox.backend.domain.model.anime.AnimeMedia
import club.anifox.backend.domain.model.anime.AnimeStudio
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeEpisodeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeRelationLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.domain.repository.anime.AnimeRepository
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.service.anime.components.AnimeCommonComponent
import club.anifox.backend.service.anime.components.AnimeSearchComponent
import club.anifox.backend.service.anime.components.episodes.AnimeTranslationsComponent
import club.anifox.backend.service.anime.components.parser.AnimeParseComponent
import club.anifox.backend.service.anime.components.update.AnimeUpdateComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
        filter: AnimeSearchFilter?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studio: String?,
    ): List<AnimeLight> {
        return animeSearchComponent.getAnimeSearch(page, limit, genres, status, filter, searchQuery, season, ratingMpa, minimalAge, type, year, translations, studio)
    }

    override fun getAnimeDetails(url: String): AnimeDetail {
        return animeCommonComponent.getAnimeByUrl(url)
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

    override fun getAnimeEpisodes(url: String, page: Int, limit: Int, sort: AnimeEpisodeFilter?): List<AnimeEpisodeLight> {
        return animeCommonComponent.getAnimeEpisodes(url, page, limit, sort)
    }

    override fun getAnimeMedia(url: String): List<AnimeMedia> {
        return animeCommonComponent.getAnimeMedia(url)
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

    override fun updateEpisodes() {
        animeUpdateComponent.update()
    }

    override fun addBlocked(url: String?, shikimoriId: Int?) {
        animeCommonComponent.blockAnime(url, shikimoriId)
    }
}
