package club.anifox.backend.service.auth.components

import club.anifox.backend.domain.dto.auth.keycloak.KeycloakTokenRefreshDto
import club.anifox.backend.domain.dto.users.registration.UserCreateResponseDto
import club.anifox.backend.domain.enums.user.RoleName
import club.anifox.backend.domain.enums.user.TypeUser
import club.anifox.backend.domain.exception.common.BadRequestException
import club.anifox.backend.domain.model.user.request.CreateUserRequest
import club.anifox.backend.jpa.entity.user.RoleTable
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.RoleRepository
import club.anifox.backend.jpa.repository.user.UserRepository
import club.anifox.backend.service.keycloak.KeycloakService
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
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AuthComponent(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val keycloakService: KeycloakService,
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.resource}") private val clientId: String,
    @Value("\${keycloak.credentials.secret}") private val secret: String,
    @Value("\${keycloak.auth-server-url}") private val authServer: String,
    private val authzClient: AuthzClient,
    private val httpClient: HttpClient,
) {
    fun authenticate(userIdentifier: String, password: String, response: HttpServletResponse) {
        val user = if (userRepository.findByUsernameOrEmail(userIdentifier).isPresent) {
            userRepository.findByUsernameOrEmail(userIdentifier).get()
        } else {
            throw BadCredentialsException("User not found with user identifier: $userIdentifier")
        }
        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Invalid user identifier/password supplied")
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
            throw BadCredentialsException("Bad Request: ${e.message}}")
        }
    }

    fun refreshAccessToken(refreshToken: String, response: HttpServletResponse) {
        val parameters = Parameters.build {
            append("client_id", clientId)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_secret", secret)
        }

        val refresh = runBlocking {
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

    fun registration(signUpRequest: CreateUserRequest, response: HttpServletResponse) {
        println("SDAF = $signUpRequest")
        if (userRepository.findByEmail(signUpRequest.email).isPresent) {
            throw BadCredentialsException("Email already exists")
        }

        if (userRepository.findByLogin(signUpRequest.login).isPresent) {
            throw BadCredentialsException("Username already exists")
        }

        val user = UserRepresentation()

        user.apply {
            isEnabled = true
            username = signUpRequest.login
            email = signUpRequest.email
        }

        val role =
            if (roleRepository.findByName(RoleName.ROLE_USER).isPresent) {
                roleRepository.findByName(RoleName.ROLE_USER)
                    .get()
            } else {
                roleRepository.save(RoleTable(name = RoleName.ROLE_USER))
            }

        val realmResource = keycloak.realm(realm)
        val usersResource = realmResource.users()
        val userCreateResponseDto = UserCreateResponseDto()

        try {
            usersResource.create(user).use { res ->
                userCreateResponseDto.statusCode = res.status
                userCreateResponseDto.status = res.statusInfo.toString()
                println(res.status)
                println(res.cookies)
                if (res.status == HttpStatus.CREATED.value()) {
                    val userId = CreatedResponseUtil.getCreatedId(res)
                    println(userId)
                    userCreateResponseDto.userId = userId
                    val passwordCred = keycloakService.getCredentialRepresentation(signUpRequest.password)
                    println("WTF = ${usersResource[userId]}")
                    println("WTF = $passwordCred")
                    val userResource = usersResource[userId]
                    println("WTF = ${userResource.credentials()}")
                    userResource.resetPassword(passwordCred)
                    println("ZXC")
                    insertNewRole(role.name.name, realmResource, userResource)
                    println("ZXCWER")

                    val userEntity = UserTable(
                        email = signUpRequest.email,
                        login = signUpRequest.login,
                        password = passwordEncoder.encode(signUpRequest.password),
                        roles = mutableSetOf(),
                        image = "",
                        nickName = signUpRequest.nickname,
                        typeUser = TypeUser.AniFox,
                    )

                    println("HERE SAFED!@#")

                    userEntity.roles.add(role)
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

    private fun insertNewRole(
        newRole: String,
        realmResource: RealmResource,
        userResource: UserResource,
    ) {
        val realmRoleUser: RoleRepresentation = realmResource.roles()[newRole].toRepresentation()
        userResource.roles().realmLevel().add(listOf(realmRoleUser))
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
        cookieAccess.domain = "anifox.club"
        cookieAccess.path = "/"

        val cookieRefresh = Cookie("refresh_token", refreshToken)
        cookieRefresh.maxAge = refreshExpires.toInt()
        cookieRefresh.domain = "anifox.club"
        cookieRefresh.path = "/"

        response.addCookie(cookieAccess)
        response.addCookie(cookieRefresh)
    }
}
