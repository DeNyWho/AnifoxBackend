package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.mappers.anime.detail.toAnimeDetail
import club.anifox.backend.domain.model.anime.detail.AnimeDetail
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import club.anifox.backend.util.AnimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeCommonComponent {

    @Autowired
    private lateinit var animeRepository: AnimeRepository

    @Autowired
    private lateinit var animeUtils: AnimeUtils

    fun getAnimeByUrl(url: String): AnimeDetail {
        val anime = animeUtils.checkAnime(url, animeRepository)
        return anime.toAnimeDetail()
    }
}
