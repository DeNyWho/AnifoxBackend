package club.anifox.backend.domain.enums.anime.parser

enum class CompressAnimeImageType(private val width: Int, private val height: Int) {
    Large(400, 640),
    Medium(200, 440),
    Cover(800, 200),
    ;

    fun extractWidthAndHeight(): Pair<Int, Int> {
        return Pair(width, height)
    }
}
