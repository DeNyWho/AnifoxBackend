package club.anifox.backend.domain.dto.anime.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanResponseDefaultDto<T>(
    @SerialName("data")
    val data: List<T> = listOf(),
)
