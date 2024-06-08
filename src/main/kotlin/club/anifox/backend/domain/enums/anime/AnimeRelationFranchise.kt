package club.anifox.backend.domain.enums.anime

enum class AnimeRelationFranchise(val russian: String) {
    Sequel("Продолжение"),
    Prequel("Предыстория"),
    SideStory("Другая история"),
    AlternativeVersion("Альтернативная история"),
    Summary("Обобщение"),
    Other("Прочее"),
    Adaptation("Адаптация"),
    AlternativeSetting("Альтернативная вселенная"),
    SpinOff("Ответвление от оригинала"),
    Character("Общий персонаж"),
}
