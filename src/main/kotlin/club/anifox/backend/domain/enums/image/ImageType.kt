package club.anifox.backend.domain.enums.image

enum class ImageType {
    PNG,
    JPG,
    WEBP,
    ;

    fun textFormat(): String {
        return when (this) {
            PNG -> "png"
            JPG -> "jpg"
            WEBP -> "webp"
        }
    }
}
