package club.anifox.backend.config.web.converter.anime.common

import club.anifox.backend.domain.enums.anime.AnimeStatus
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class AnimeStatusConverter : Converter<String, AnimeStatus> {
    override fun convert(source: String): AnimeStatus {
        return try {
            AnimeStatus.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid AnimeSearchFilter value: $source. " +
                    "Allowed values are: ${AnimeStatus.entries.joinToString(", ")}",
            )
        }
    }
}
