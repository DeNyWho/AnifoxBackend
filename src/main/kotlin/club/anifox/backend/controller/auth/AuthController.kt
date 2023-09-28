package club.anifox.backend.controller.auth

import club.anifox.backend.domain.model.user.request.AuthenticationRequest
import club.anifox.backend.domain.model.user.request.CreateUserRequest
import club.anifox.backend.service.auth.AuthService
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin("*")
@Tag(name = "AuthApi", description = "All about auth")
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/authentication")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [
                    Content(
                        examples = [
                            ExampleObject(
                                name = "Error v1",
                                value = """{"error": "User not found with email: example@example.example"}""",
                                description = "Auth error email not exists",
                            ),
                            ExampleObject(
                                name = "Error v2",
                                value = """{"error": "Invalid email/password supplied"}""",
                                description = "Auth error wrong email or password",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "201",
                description = "Authorized (Data -> cookie)",
            ),
        ],
    )
    fun authenticate(
        @Valid @RequestBody
        loginRequest: AuthenticationRequest,
        res: HttpServletResponse,
    ) {
        return authService.authenticate(loginRequest = loginRequest, res)
    }

    @PostMapping("/registration")
    fun registration(@RequestBody signUpRequest: CreateUserRequest, response: HttpServletResponse) {
        println("WWW")
        return authService.registration(signUpRequest, response)
    }

    @GetMapping("/refreshToken")
    fun refreshAccessToken(refreshToken: String, response: HttpServletResponse) {
        return authService.refreshAccessToken(refreshToken, response)
    }
}
