package club.anifox.backend.service.anime.components.parser

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*
import java.util.regex.Pattern

@Component
class CommonParserComponent {
    fun checkEnglishLetter(titleRussian: String?): Boolean {
        return if (titleRussian != null) {
            val pattern = Pattern.compile("[a-zA-Z]")
            val matcher = pattern.matcher(titleRussian)
            matcher.find()
        } else {
            false
        }
    }

    fun translit(str: String): String {
        val charMap =
            mapOf(
                'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d", 'е' to "e",
                'ё' to "yo", 'є' to "ie", 'ж' to "zh", 'з' to "z", 'и' to "i", 'і' to "i", 'ї' to "i", 'й' to "i",
                'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
                'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
                'ш' to "sh", 'щ' to "shch", 'ы' to "y", 'ь' to "", 'ъ' to "", 'э' to "e", 'ю' to "iu", 'я' to "ia",
                'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Ґ' to "G", 'Д' to "D", 'Е' to "E",
                'Ё' to "Yo", 'Є' to "Ye", 'Ж' to "Zh", 'З' to "Z", 'И' to "I", 'І' to "I", 'Ї' to "Yi", 'Й' to "Y",
                'К' to "K", 'Л' to "L", 'М' to "M", 'Н' to "N", 'О' to "O", 'П' to "P", 'Р' to "R",
                'С' to "S", 'Т' to "T", 'У' to "U", 'Ф' to "F", 'Х' to "Kh", 'Ц' to "Ts", 'Ч' to "Ch",
                'Ш' to "Sh", 'Щ' to "Shch", 'Ы' to "Y", 'Ь' to "", 'Ъ' to "", 'Э' to "E", 'Ю' to "Yu", 'Я' to "Ya",
            )

        return str.lowercase(Locale.getDefault())
            .map { charMap[it] ?: if (it.isLetterOrDigit()) it.toString() else "-" }
            .joinToString("")
            .dropLastWhile { it == '-' }
    }

    fun getMostCommonColor(image: BufferedImage): String {
        val brightestColors = getBrightestColors(image)
        val numColors = brightestColors.size

        if (numColors == 0) {
            return "#FFFFFF"
        }

        val red = brightestColors.sumOf { it.red } / numColors
        val green = brightestColors.sumOf { it.green } / numColors
        val blue = brightestColors.sumOf { it.blue } / numColors

        return String.format("#%02X%02X%02X", red, green, blue)
    }

    private fun getBrightestColors(image: BufferedImage): List<Color> {
        val colorCount = mutableMapOf<Int, Int>()

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                val red = (pixel shr 16 and 0xFF) / 255.0
                val green = (pixel shr 8 and 0xFF) / 255.0
                val blue = (pixel and 0xFF) / 255.0

                if (red > 0.9 && green > 0.9 && blue > 0.9) {
                    continue // Белый цвет
                }
                if (red < 0.4 && green < 0.4 && blue < 0.4) {
                    continue // Черный цвет
                }
                if (red > 0.9 && green > 0.9 && blue > 0.8) {
                    continue // Очень светлый цвет
                }

                colorCount[pixel] = colorCount.getOrDefault(pixel, 0) + 1
            }
        }

        val mostCommonPixels = colorCount.filter { it.value > 30 }.keys

        return mostCommonPixels.map { Color(it) }.sortedByDescending { colorBrightness(it) }
    }

    private fun colorBrightness(color: Color): Double {
        return (color.red * 299 + color.green * 587 + color.blue * 114) / 1000.0
    }
}
