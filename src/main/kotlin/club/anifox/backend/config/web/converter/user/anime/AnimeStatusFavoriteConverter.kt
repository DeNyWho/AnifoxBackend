package club.anifox.backend.config.web.converter.user.anime

import club.anifox.backend.domain.enums.user.StatusFavourite
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class AnimeStatusFavoriteConverter : Converter<String, StatusFavourite> {
    override fun convert(source: String): StatusFavourite {
        return try {
            StatusFavourite.entries.first { it.jsonKey.equals(source, ignoreCase = true) }
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException(
                "Invalid StatusFavourite value: $source. " +
                    "Allowed values are: ${StatusFavourite.entries.joinToString(", ") { it.jsonKey }}",
            )
        }
    }
}
