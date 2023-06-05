package com.example.backend.controller.auth

import com.example.backend.api.ApiError
import com.example.backend.models.users.*
import com.example.backend.service.user.AuthService
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@RestController
@CrossOrigin("*")
@Tag(name = "AuthApi", description = "All about auth")
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
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
                                description = "Auth error email not exists"
                            ),
                            ExampleObject(
                                name = "Error v2",
                                value = """{"error": "Invalid email/password supplied"}""",
                                description = "Auth error wrong email or password"
                            ),
                        ],
                        schema = Schema(implementation = ApiError::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "200",
                description = "Authorized (Data -> cookie)"
            ),
        ]
    )
    fun login(@Valid @RequestBody loginRequest: LoginRequest, res: HttpServletResponse): TokenResponse {
        return authService.authenticate(email = loginRequest.email, password = loginRequest.password, res)
    }

    @PostMapping("/register")
    fun register(@RequestBody signUpRequest: SignUpRequest, response: HttpServletResponse): TokenResponse {
        return authService.register(signUpRequest, response)
    }

    @Value("\${shikimori.oauth.client-id}")
    private lateinit var clientId: String

    @Value("\${shikimori.oauth.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${host_url}")
    private lateinit var hostUrl: String

    @GetMapping("/shikimori/login")
    fun shikimoriLogin(): String {
        return "https://shikimori.one/oauth/authorize" +
                "?client_id=$clientId" +
                "&redirect_uri=$hostUrl/api/auth/oauth2/code/shikimori" +
                "&response_type=code" +
                "&scope=user_rates"
    }

    @GetMapping("/oauth2/code/shikimori")
    fun handleOAuth2CodeShikimori(@RequestParam("code") code: String, response: HttpServletResponse): RedirectView {
        return authService.authenticateShikimori(code, response)
    }

}

//    @GetMapping("/refreshToken")
//    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse) {
//        val refreshTokenString = sessionService.getRefreshToken(request)
//        if (refreshTokenString != null) {
//            val accessTokenResponse = keycloak.tokenManager() (refreshTokenString)
//            sessionService.updateSession(accessTokenResponse, response)
//            response.status = HttpStatus.OK.value()
//            return
//        }
//        response.status = HttpStatus.UNAUTHORIZED.value()
//    }

//    @PostMapping("/oauth/token")
//    fun exchangeKeycloakToken(@RequestParam("grant_type") grantType: String,
//                              @RequestParam("refresh_token") refreshToken: String,
//                              response: HttpServletResponse) {
//        if (grantType != "refresh_token") {
//            response.status = HttpStatus.BAD_REQUEST.value()
//            return
//        }
//
//        val accessTokenResponse = keycloak.tokenManager().accessToken
//            ?: throw BadRequestException("Invalid refresh token")
//        sessionService.createSession(accessTokenResponse.otherClaims.get(), response)
//        response.contentType = "application/json"
//        response.writer.write("""
//        {
//            "access_token": "${accessTokenResponse.token}",
//            "token_type": "bearer",
//            "refresh_token": "${accessTokenResponse.refreshToken}"
//        }
//        """.trimIndent())
//        response.status = HttpStatus.OK.value()
//    }
