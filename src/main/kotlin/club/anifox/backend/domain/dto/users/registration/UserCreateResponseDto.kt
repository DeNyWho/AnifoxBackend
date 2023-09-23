package club.anifox.backend.domain.dto.users.registration

data class UserCreateResponseDto(
    var userId: String? = null,
    var statusCode: Int = 0,
    var status: String? = null,
)
