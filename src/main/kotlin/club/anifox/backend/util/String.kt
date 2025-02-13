package club.anifox.backend.util

import club.anifox.backend.domain.enums.common.LanguageType

fun String.replaceLast(oldValue: String, newValue: String): String {
    val lastIndex = lastIndexOf(oldValue)
    if (lastIndex == -1) {
        return this
    }
    val prefix = substring(0, lastIndex)
    val suffix = substring(lastIndex + oldValue.length)
    return "$prefix$newValue$suffix"
}

fun String.detectLanguage(): LanguageType {
    val japaneseRegex = Regex("[ぁ-んァ-ン一-龥]")
    val russianRegex = Regex("[А-Яа-я]")
    val englishRegex = Regex("[A-Za-z]")

    val japaneseCount = japaneseRegex.findAll(this).count()
    val russianCount = russianRegex.findAll(this).count()
    val englishCount = englishRegex.findAll(this).count()

    return when {
        japaneseCount > englishCount && japaneseCount > russianCount -> LanguageType.JAPANESE
        russianCount > japaneseCount && russianCount > englishCount -> LanguageType.RUSSIAN
        englishCount > japaneseCount && englishCount > russianCount -> LanguageType.ENGLISH
        else -> LanguageType.UNKNOWN
    }
}
