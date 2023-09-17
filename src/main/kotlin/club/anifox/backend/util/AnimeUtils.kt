package club.anifox.backend.util

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.repository.anime.AnimeRepository
import org.springframework.stereotype.Component

@Component
object AnimeUtils {

    fun checkAnime(url: String, animeRepository: AnimeRepository): AnimeTable {
        return animeRepository.findByUrl(url).orElseThrow { NotFoundException("Anime not found") }
    }
}
