package club.anifox.backend.config.web

import club.anifox.backend.config.web.converter.anime.common.AnimeStatusConverter
import club.anifox.backend.config.web.converter.anime.common.AnimeTypeConverter
import club.anifox.backend.config.web.converter.anime.filter.AnimeDefaultFilterConverter
import club.anifox.backend.config.web.converter.anime.search.AnimeSearchFilterConverter
import club.anifox.backend.config.web.converter.user.anime.AnimeStatusFavoriteConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    @Bean
    fun animeSearchFilterConverter(): AnimeSearchFilterConverter {
        return AnimeSearchFilterConverter()
    }

    @Bean
    fun animeStatusConverter(): AnimeStatusConverter {
        return AnimeStatusConverter()
    }

    @Bean
    fun animeDefaultFilterConverter(): AnimeDefaultFilterConverter {
        return AnimeDefaultFilterConverter()
    }

    @Bean
    fun animeTypeConverter(): AnimeTypeConverter {
        return AnimeTypeConverter()
    }

    @Bean
    fun animeStatusFavoriteConverter(): AnimeStatusFavoriteConverter {
        return AnimeStatusFavoriteConverter()
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(animeSearchFilterConverter())
        registry.addConverter(animeStatusConverter())
        registry.addConverter(animeDefaultFilterConverter())
        registry.addConverter(animeTypeConverter())
        registry.addConverter(animeStatusFavoriteConverter())
    }
}
