package club.anifox.backend.domain.dto.anime.kitsu.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuEpisodeTitle(
    @SerialName("en_jp")
    val enToJp: String? = null,
    @SerialName("en_us")
    val enToUs: String? = null,
    @SerialName("en")
    val en: String? = null,
    @SerialName("ja_jp")
    val original: String? = null,
)
