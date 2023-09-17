package club.anifox.backend.service.anime

import club.anifox.backend.domain.enums.anime.AnimeOrder
import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.domain.model.anime.translation.AnimeTranslationCount
import club.anifox.backend.domain.repository.anime.AnimeRepository
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable
import club.anifox.backend.service.anime.components.AnimeCommonComponent
import club.anifox.backend.service.anime.components.AnimeSearchComponent
import club.anifox.backend.service.anime.components.AnimeTranslationsComponent
import club.anifox.backend.service.anime.components.parser.AnimeParseComponent
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

    override fun getAnime(
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
    ): List<AnimeLight> {
        return animeSearchComponent.getAnime(pageNum, pageSize, genres, status, order, searchQuery, season, ratingMpa, minimalAge, type, year, translations, studio)
    }

    override fun getAnimeDetails(url: String): AnimeDetail {
        return animeCommonComponent.getAnimeByUrl(url)
    }

    override fun getAnimeTranslationsCount(url: String): List<AnimeTranslationCount> {
        return animeTranslationsComponent.getAnimeTranslationsCount(url)
    }

    override fun getAnimeTranslations(): List<AnimeTranslationTable> {
        return animeTranslationsComponent.getAnimeTranslations()
    }

    override fun parseTranslations(transltionsIDs: List<Int>) {
        animeTranslationsComponent.addTranslationsToDB(transltionsIDs)
    }

    override fun parseAnime(transltionsIDs: String) {
        animeParseComponent.addDataToDB(transltionsIDs)
    }
}
