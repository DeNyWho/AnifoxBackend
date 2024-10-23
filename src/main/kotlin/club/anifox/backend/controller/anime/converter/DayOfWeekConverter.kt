package club.anifox.backend.controller.anime.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.time.DayOfWeek

@Component
class DayOfWeekConverter : Converter<String, DayOfWeek> {
    override fun convert(source: String): DayOfWeek? {
        return try {
            // Приводим входную строку к верхнему регистру для соответствия enum
            DayOfWeek.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
