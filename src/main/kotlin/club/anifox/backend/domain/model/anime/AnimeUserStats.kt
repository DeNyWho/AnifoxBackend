package club.anifox.backend.domain.model.anime

import club.anifox.backend.domain.enums.user.StatusFavourite
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeUserStats
@OptIn(ExperimentalSerializationApi::class)
constructor(
    @SerialName("is_fav")
    @EncodeDefault
    val isFavourite: Boolean = false,
    @SerialName("list")
    @EncodeDefault
    val list: StatusFavourite? = null,
    @SerialName("rating")
    @EncodeDefault
    val rating: Int? = null,
)
