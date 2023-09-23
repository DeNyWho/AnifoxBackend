package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriFavoriteListDto(
    val name: String,
    val value: Int,
)
