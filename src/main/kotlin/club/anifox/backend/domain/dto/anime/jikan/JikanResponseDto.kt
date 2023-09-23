package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanResponseDto<T>(
    @SerialName("data")
    val data: T? = null,
)
