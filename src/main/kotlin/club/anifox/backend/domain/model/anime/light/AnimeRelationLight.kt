package club.anifox.backend.domain.model.anime.light

import club.anifox.backend.domain.model.anime.AnimeRelation
import kotlinx.serialization.Serializable

@Serializable
data class AnimeRelationLight(
    val anime: AnimeLight,
    val relation: AnimeRelation,
)
