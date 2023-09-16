package club.anifox.backend.domain.dto.anime.kodik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodikResponseDto<T>(
    @SerialName("time")
    val time: String = "",
    @SerialName("next_page")
    val nextPage: String? = null,
    @SerialName("results")
    val result: List<T> = listOf(),
)
