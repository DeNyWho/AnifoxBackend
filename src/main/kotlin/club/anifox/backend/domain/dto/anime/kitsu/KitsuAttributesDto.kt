package club.anifox.backend.domain.dto.anime.kitsu

import club.anifox.backend.domain.dto.anime.kitsu.image.KitsuCoverImageDto
import club.anifox.backend.domain.dto.anime.kitsu.image.KitsuPosterImageDto
import kotlinx.serialization.Serializable

@Serializable
data class KitsuAttributesDto(
    val posterImage: KitsuPosterImageDto = KitsuPosterImageDto(),
    val coverImage: KitsuCoverImageDto = KitsuCoverImageDto(),
)
