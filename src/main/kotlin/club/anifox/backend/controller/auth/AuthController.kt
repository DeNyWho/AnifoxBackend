package club.anifox.backend.controller.auth

import club.anifox.backend.domain.model.user.request.AuthenticationRequest
import club.anifox.backend.domain.model.user.request.CreateUserRequest
import club.anifox.backend.service.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin("*")
@Tag(name = "AuthApi", description = "All about auth")
@RequestMapping("/api/auth/")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("authentication")
    @Operation(
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = [
                Content(
                    mediaType = "application/json",
                    examples = [
                        ExampleObject(
                            value = "{ \"user_identifier\": " +
                                "\"user OR email\"," +
                                "\"password\": \"String123!\" }",
                        ),
                    ],
                ),
            ],
        ),
    )
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
        @RequestBody
        loginRequest: AuthenticationRequest,
        res: HttpServletResponse,
    ) {
        return authService.authenticate(loginRequest = loginRequest, res)
    }

    @PostMapping("check/email")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Email is available",
            ),
            ApiResponse(
                responseCode = "409",
                description = "The email already exists in the system",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid email format",
            ),
        ],
    )
    fun checkEmail(email: String, response: HttpServletResponse) {
        return authService.checkEmail(email, response)
    }

    @PostMapping("check/login")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login is available",
            ),
            ApiResponse(
                responseCode = "409",
                description = "The login already exists in the system",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid login format",
            ),
        ],
    )
    fun checkLogin(login: String, response: HttpServletResponse) {
        return authService.checkLogin(login, response)
    }

    @PostMapping("registration")
    fun registration(@RequestBody signUpRequest: CreateUserRequest, response: HttpServletResponse) {
        return authService.registration(signUpRequest, response)
    }

    @GetMapping("refreshToken")
    fun refreshAccessToken(refreshToken: String, response: HttpServletResponse) {
        return authService.refreshAccessToken(refreshToken, response)
    }
}
