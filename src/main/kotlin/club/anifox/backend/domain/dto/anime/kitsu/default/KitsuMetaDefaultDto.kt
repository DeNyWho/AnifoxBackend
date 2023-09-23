package club.anifox.backend.domain.dto.anime.kitsu.default

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuMetaDefaultDto(
    @SerialName("count")
    val count: Int? = null,
)
