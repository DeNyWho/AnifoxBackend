package club.anifox.backend.domain.exception.common

class BadRequestException(override val message: String?) : Exception(message)
