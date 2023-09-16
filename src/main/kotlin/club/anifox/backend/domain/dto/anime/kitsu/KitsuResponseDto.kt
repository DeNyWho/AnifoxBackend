package club.anifox.backend.domain.dto.anime.kitsu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuResponseDto<T>(
    @SerialName("data")
    var data: T? = null,
)
