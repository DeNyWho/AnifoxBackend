package club.anifox.backend.domain.dto.anime.kitsu

import club.anifox.backend.domain.dto.anime.kitsu.default.KitsuLinkDefaultDto
import club.anifox.backend.domain.dto.anime.kitsu.default.KitsuMetaDefaultDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuDefaultResponseDto<T>(
    @SerialName("data")
    var data: List<T>? = null,
    @SerialName("meta")
    val meta: KitsuMetaDefaultDto = KitsuMetaDefaultDto(),
    @SerialName("links")
    val links: KitsuLinkDefaultDto = KitsuLinkDefaultDto(),
)
