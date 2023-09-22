package club.anifox.backend.domain.repository.auth

import club.anifox.backend.domain.model.user.request.AuthenticationRequest
import club.anifox.backend.domain.model.user.request.CreateUserRequest
import jakarta.servlet.http.HttpServletResponse

interface AuthRepository {
    fun authenticate(loginRequest: AuthenticationRequest, res: HttpServletResponse)
    fun registration(signUpRequest: CreateUserRequest, response: HttpServletResponse)
    fun refreshAccessToken(refreshToken: String, response: HttpServletResponse)
}
