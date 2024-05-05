package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriGenresDto(
    @SerialName("id")
    val id: Int,
    @SerialName("russian")
    val russian: String,
)
