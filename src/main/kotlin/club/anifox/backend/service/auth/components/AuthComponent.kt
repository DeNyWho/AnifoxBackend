package club.anifox.backend.service.auth.components

import club.anifox.backend.domain.dto.auth.keycloak.KeycloakTokenRefreshDto
import club.anifox.backend.domain.dto.users.registration.UserCreateResponseDto
import club.anifox.backend.domain.enums.user.UserIdentifierType
import club.anifox.backend.domain.exception.common.BadRequestException
import club.anifox.backend.domain.exception.common.ConflictException
import club.anifox.backend.domain.model.user.request.CreateUserRequest
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.UserRepository
import club.anifox.backend.service.keycloak.KeycloakService
import club.anifox.backend.util.user.UserUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import java.util.*
import kotlin.random.Random

@Component
class AuthComponent(
    private val userRepository: UserRepository,
    private val keycloakService: KeycloakService,
    private val keycloak: Keycloak,
    private val userUtils: UserUtils,
    @Value("\${domain}") private val domain: String,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.resource}") private val clientId: String,
    @Value("\${keycloak.credentials.secret}") private val secret: String,
    @Value("\${keycloak.auth-server-url}") private val authServer: String,
    private val authzClient: AuthzClient,
    private val httpClient: HttpClient,
) {
    fun authenticate(
        userIdentifier: String,
        password: String,
        response: HttpServletResponse,
    ) {
        when (userUtils.checkUserIdentifier(userIdentifier)) {
            UserIdentifierType.EMAIL -> {
                if (keycloakService.findByEmail(userIdentifier) == null) {
                    throw BadCredentialsException("User with email $userIdentifier not exist")
                }
            }
            UserIdentifierType.LOGIN -> {
                if (keycloakService.findByUsername(userIdentifier) == null) {
                    throw BadCredentialsException("User with username $userIdentifier not exist")
                }
            }
        }

        try {
            val auth = authzClient.obtainAccessToken(userIdentifier, password)

            makeCookieAniFox(
                response = response,
                accessToken = auth.token,
                accessExpires = auth.expiresIn,
                refreshToken = auth.refreshToken,
                refreshExpires = auth.refreshExpiresIn,
            )
        } catch (e: Exception) {
            throw BadCredentialsException("Wrong user identifier or password")
        }
    }

    fun refreshAccessToken(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        val parameters =
            Parameters.build {
                append("client_id", clientId)
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_secret", secret)
            }

        val refresh =
            runBlocking {
                httpClient.post("${authServer}realms/$realm/protocol/openid-connect/token") {
                    headers {
                        contentType(ContentType.Application.FormUrlEncoded)
                    }
                    setBody(FormDataContent(parameters))
                }.body<KeycloakTokenRefreshDto>()
            }

        makeCookieAniFox(
            response = response,
            accessToken = refresh.accessToken,
            refreshToken = refresh.refreshToken,
            accessExpires = refresh.expiresIn,
            refreshExpires = refresh.refreshExpiresIn,
        )
    }

    fun registration(
        signUpRequest: CreateUserRequest,
        response: HttpServletResponse,
    ) {
        validateEmail(signUpRequest.email)
        validateLogin(signUpRequest.login)

        val user = UserRepresentation()

        user.apply {
            isEnabled = true
            username = signUpRequest.login
            email = signUpRequest.email
        }

        val realmResource = keycloak.realm(realm)
        val usersResource = realmResource.users()
        val userCreateResponseDto = UserCreateResponseDto()

        try {
            usersResource.create(user).use { res ->
                userCreateResponseDto.statusCode = res.status
                userCreateResponseDto.status = res.statusInfo.toString()
                if (res.status == HttpStatus.CREATED.value()) {
                    val userId = CreatedResponseUtil.getCreatedId(res)

                    userCreateResponseDto.userId = userId
                    val passwordCred = keycloakService.getCredentialRepresentation(signUpRequest.password)
                    val userResource = usersResource[userId]
                    userResource.resetPassword(passwordCred)

                    val userEntity =
                        UserTable(
                            id = userId,
                            login = signUpRequest.login,
                            image = "",
                            birthday = signUpRequest.birthday,
                            nickName = "user${(1..10).map { Random.nextInt(0, 10) }.joinToString("")}",
                        )

                    userRepository.save(userEntity)
                }
            }
        } catch (e: Exception) {
            println(e.message)
            throw BadRequestException("${e.message}")
        }

        val auth = authzClient.obtainAccessToken(signUpRequest.login, signUpRequest.password)

        makeCookieAniFox(
            response = response,
            accessToken = auth.token,
            refreshToken = auth.refreshToken,
            accessExpires = auth.expiresIn,
            refreshExpires = auth.refreshExpiresIn,
        )

        response.status = HttpStatus.CREATED.value()
    }

    fun checkEmail(
        email: String,
        response: HttpServletResponse,
    ) {
        validateEmail(email)

        response.status = HttpStatus.OK.value()
        response.writer.write("Email is available")
    }

    fun checkLogin(
        login: String,
        response: HttpServletResponse,
    ) {
        validateLogin(login)

        response.status = HttpStatus.OK.value()
        response.writer.write("Login is available")
    }

    private fun makeCookieAniFox(
        response: HttpServletResponse,
        accessToken: String,
        refreshToken: String,
        accessExpires: Long,
        refreshExpires: Long,
    ) {
        val cookieAccess = Cookie("access_token", accessToken)
        cookieAccess.maxAge = accessExpires.toInt()
        cookieAccess.domain = domain
        cookieAccess.path = "/"

        val cookieRefresh = Cookie("refresh_token", refreshToken)
        cookieRefresh.maxAge = refreshExpires.toInt()
        cookieRefresh.domain = domain
        cookieRefresh.path = "/"

        response.addCookie(cookieAccess)
        response.addCookie(cookieRefresh)
    }

    private fun validateEmail(email: String) {
        if (!isValidEmailFormat(email)) {
            throw BadRequestException("Invalid email format")
        }

        if (emailExistsInSystem(email)) {
            throw BadCredentialsException("The email already exists in the system")
        }
    }

    private fun isValidEmailFormat(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9]+([._-]?[a-zA-Z0-9]+)*@[a-zA-Z0-9]+\\.[a-zA-Z]{2,6}\$".toRegex()
        return email.matches(emailRegex)
    }

    private fun emailExistsInSystem(email: String): Boolean {
        return keycloakService.findByEmail(email) != null
    }

    private fun validateLogin(login: String) {
        if (!isValidLoginFormat(login)) {
            throw BadRequestException("Invalid login format")
        }

        if (loginExistsInSystem(login)) {
            throw ConflictException("The login already exists in the system")
        }
    }

    private fun isValidLoginFormat(login: String): Boolean {
        val loginRegex = "^[a-zA-Z][a-zA-Z0-9_]{3,15}$".toRegex()
        return login.matches(loginRegex)
    }

    private fun loginExistsInSystem(login: String): Boolean {
        return keycloakService.findByUsername(login) != null
    }
}
