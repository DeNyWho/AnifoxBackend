package club.anifox.backend.util

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeRepository

object AnimeUtils {

    private lateinit var animeRepository: AnimeRepository

    fun checkAnime(url: String): AnimeTable {
        return animeRepository.findByUrl(url).orElseThrow { NotFoundException("Anime not found") }
    }
}
