package club.anifox.backend.config.web.converter.anime.common

import club.anifox.backend.domain.enums.anime.AnimeType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class AnimeTypeConverter : Converter<String, AnimeType> {
    override fun convert(source: String): AnimeType {
        return try {
            AnimeType.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid AnimeSearchFilter value: $source. " +
                    "Allowed values are: ${AnimeType.entries.joinToString(", ")}",
            )
        }
    }
}
