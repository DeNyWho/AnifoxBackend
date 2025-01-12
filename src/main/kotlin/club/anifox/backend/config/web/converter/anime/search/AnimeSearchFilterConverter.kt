package club.anifox.backend.config.web.converter.anime.search

import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class AnimeSearchFilterConverter : Converter<String, AnimeSearchFilter> {
    override fun convert(source: String): AnimeSearchFilter {
        return try {
            AnimeSearchFilter.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid AnimeSearchFilter value: $source. " +
                    "Allowed values are: ${AnimeSearchFilter.entries.joinToString(", ")}",
            )
        }
    }
}
