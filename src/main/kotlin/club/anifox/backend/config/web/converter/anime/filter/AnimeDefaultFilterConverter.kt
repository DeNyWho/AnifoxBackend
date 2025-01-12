package club.anifox.backend.config.web.converter.anime.filter

import club.anifox.backend.domain.enums.anime.filter.AnimeDefaultFilter
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class AnimeDefaultFilterConverter : Converter<String, AnimeDefaultFilter> {
    override fun convert(source: String): AnimeDefaultFilter {
        return try {
            AnimeDefaultFilter.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid AnimeSearchFilter value: $source. " +
                    "Allowed values are: ${AnimeDefaultFilter.entries.joinToString(", ")}",
            )
        }
    }
}
