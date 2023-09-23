package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanPaginationDto(
    @SerialName("last_visible_page")
    val lastVisiblePage: Int = 0,
    @SerialName("has_next_page")
    val hasNextPage: Boolean = true,
)
