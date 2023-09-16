package club.anifox.backend.domain.dto.anime.shikimori

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriRelationDto(
    @SerialName("relation")
    val relation: String? = null,
    @SerialName("relation_russian")
    val relationRussian: String? = null,
    val anime: ShikimoriAnimeIdDto? = null,
    val manga: ShikimoriMangaIdDto? = null,
)

@Serializable
data class ShikimoriAnimeIdDto(
    val id: Int,
)

@Serializable
data class ShikimoriMangaIdDto(
    val id: Int,
)
