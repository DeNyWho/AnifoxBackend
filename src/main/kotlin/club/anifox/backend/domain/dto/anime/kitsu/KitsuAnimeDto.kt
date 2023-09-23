package club.anifox.backend.domain.dto.anime.kitsu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuAnimeDto(
    @SerialName("attributes")
    val attributesKitsu: KitsuAttributesDto = KitsuAttributesDto(),
)
