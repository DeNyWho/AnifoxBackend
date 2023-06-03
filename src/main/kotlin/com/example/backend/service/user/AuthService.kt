package com.example.backend.service.user

import com.example.backend.jpa.shikimori.ShikimoriUsers
import com.example.backend.jpa.user.Role
import com.example.backend.jpa.user.RoleName
import com.example.backend.jpa.user.User
import com.example.backend.models.shikimori.ShikimoriOauth
import com.example.backend.models.shikimori.ShikimoriProfile
import com.example.backend.models.users.SignUpRequest
import com.example.backend.models.users.TypeUser
import com.example.backend.models.users.UserCreateResponseDTO
import com.example.backend.repository.shikimori.ShikimoriUsersRepository
import com.example.backend.repository.user.RoleRepository
import com.example.backend.repository.user.UserRepository
import com.example.backend.service.keycloak.KeycloakService
import com.example.backend.util.exceptions.BadRequestException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
import org.springframework.stereotype.Service
import org.springframework.web.servlet.view.RedirectView
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse


@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val keycloakService: KeycloakService,
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm}") private val realm: String,
    private val authzClient: AuthzClient,
    private val shikimoriUsersRepository: ShikimoriUsersRepository
) {
    fun authenticate(email: String, password: String, response: HttpServletResponse) {
        val user = if (userRepository.findByEmail(email).isPresent) userRepository.findByEmail(email).get()
        else {
            throw BadCredentialsException("User not found with email: $email")
        }
        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Invalid email/password supplied")
        }
        try {
            makeCookieAniFox(response, email, password)

            response.status = HttpStatus.OK.value()
        } catch (e: Exception) {
            throw BadCredentialsException("Bad Request: ${e.message}}")
        }
    }

    fun register(signUpRequest: SignUpRequest, response: HttpServletResponse) {
        if (userRepository.findByEmail(signUpRequest.email).isPresent)
            throw BadCredentialsException("Email already exists")

        if (userRepository.findByUsername(signUpRequest.username).isPresent)
            throw BadCredentialsException("Username already exists")

        val user = UserRepresentation()

        user.apply {
            isEnabled = true
            username = signUpRequest.username
            email = signUpRequest.email
        }

        val role =
            if (roleRepository.findByName(RoleName.ROLE_USER).isPresent) roleRepository.findByName(RoleName.ROLE_USER)
                .get() else roleRepository.save(Role(name = RoleName.ROLE_USER))

        val realmResource = keycloak.realm(realm)
        val usersResource = realmResource.users()
        val userCreateResponseDto = UserCreateResponseDTO()

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
                    insertNewRole(role.name.name, realmResource, userResource)

                    val userEntity = User(
                        email = signUpRequest.email,
                        username = signUpRequest.username,
                        password = passwordEncoder.encode(signUpRequest.password),
                        roles = mutableSetOf(),
                        nickName = signUpRequest.nickName,
                        typeUser = TypeUser.AniFox
                    )

                    userEntity.roles.add(role)
                    userRepository.save(userEntity)

                }
            }
        } catch (e: Exception) {
            throw BadRequestException("${e.message}")
        }

        makeCookieAniFox(response, signUpRequest.username, signUpRequest.password)
        response.status = HttpStatus.CREATED.value()
    }

    fun test(response: HttpServletResponse) {
        val user = UserRepresentation()

        user.apply {
            isEnabled = true
            username = "sdfsdfsfds"
        }

        val tempPass = UUID.randomUUID().toString()

        val realmResource = keycloak.realm(realm)
        val usersResource = realmResource.users()

        try {
            val u = usersResource.searchByUsername("sdfsdfsfds", true)[0]
            val userResource = usersResource[u.id]
            val passwordCred = keycloakService.getCredentialRepresentation(tempPass)
            userResource.resetPassword(passwordCred)

            makeCookieAniFox(response, "sdfsdfsfds", tempPass)

        } catch (e: Exception) {
            throw BadRequestException("${e.message}")
        }
    }

    private fun insertNewRole(
        newRole: String,
        realmResource: RealmResource,
        userResource: UserResource
    ) {
        val realmRoleUser: RoleRepresentation = realmResource.roles()[newRole].toRepresentation()
        userResource.roles().realmLevel().add(listOf(realmRoleUser))
    }

    private fun removeOldRoles(
        newRole: String,
        realmResource: RealmResource,
        userResource: UserResource
    ) {
        val roleRepresentationList: MutableList<RoleRepresentation> = ArrayList()
        RoleName.stream()
            .filter { i -> i.name != newRole.uppercase(Locale.getDefault()) }
            .forEach { i ->
                val realmRoleUser = realmResource.roles()[i.name].toRepresentation()
                roleRepresentationList.add(realmRoleUser)
            }
        userResource.roles().realmLevel().remove(roleRepresentationList)
    }

    val client = HttpClient {
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    @Value("\${shikimori.oauth.client-id}")
    private lateinit var clientId: String

    @Value("\${shikimori.oauth.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${host_url}")
    private lateinit var hostUrl: String

    fun authenticateShikimori(code: String, response: HttpServletResponse): RedirectView {
        val shikimoriOauth = runBlocking {
            client.post {
                headers {
                    contentType(ContentType.Application.Json)
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "shikimori.me/oauth/token"
                }
                parameter("grant_type", "authorization_code")
                parameter("client_id", clientId)
                parameter("client_secret", clientSecret)
                parameter("code", code)
                parameter("redirect_uri", "$hostUrl/api/auth/oauth2/code/shikimori")
            }.body<ShikimoriOauth>()
        }

        val shikimoriWhoAmi = runBlocking {
            client.get {
                headers {
                    contentType(ContentType.Application.Json)
                    append(HttpHeaders.Authorization, "Bearer ${shikimoriOauth.accessToken}")
                }
                url {
                    protocol = URLProtocol.HTTPS
                    host = "shikimori.me/api/users/whoami"
                }
            }.body<ShikimoriProfile>()
        }
        val role =
            if (roleRepository.findByName(RoleName.ROLE_USER).isPresent) roleRepository.findByName(RoleName.ROLE_USER)
                .get() else roleRepository.save(Role(name = RoleName.ROLE_USER))
        val user = UserRepresentation()
        val realmResource = keycloak.realm(realm)
        val usersResource = realmResource.users()
        val userCreateResponseDto = UserCreateResponseDTO()
        val tempPass = UUID.randomUUID().toString()

        user.apply {
            isEnabled = true
            username = shikimoriWhoAmi.nickname
        }
        if (shikimoriUsersRepository.findById(shikimoriWhoAmi.id).isPresent) {
            val u = usersResource.searchByUsername(shikimoriWhoAmi.nickname, true)[0]
            val userResource = usersResource[u.id]
            val passwordCred = keycloakService.getCredentialRepresentation(tempPass)
            userResource.resetPassword(passwordCred)

            makeCookieAniFox(response, shikimoriWhoAmi.nickname, tempPass)
            response.status = HttpStatus.OK.value()
        } else {
            try {
                usersResource.create(user).use { res ->
                    userCreateResponseDto.statusCode = res.status
                    userCreateResponseDto.status = res.statusInfo.toString()
                    if (res.status == HttpStatus.CREATED.value()) {
                        val userId = CreatedResponseUtil.getCreatedId(res)

                        userCreateResponseDto.userId = userId
                        val userResource = usersResource[userId]
                        val passwordCred = keycloakService.getCredentialRepresentation(tempPass)
                        userResource.resetPassword(passwordCred)
                        insertNewRole(role.name.name, realmResource, userResource)

                        val userEntity = User(
                            email = null,
                            username = shikimoriWhoAmi.nickname,
                            password = null,
                            roles = mutableSetOf(),
                            typeUser = TypeUser.Shikimori,
                            image = shikimoriWhoAmi.avatar
                        )

                        userEntity.roles.add(role)
                        userRepository.save(userEntity)
                        makeCookieAniFox(response, shikimoriWhoAmi.nickname, tempPass)
                    }
                }
            } catch (e: Exception) {
                throw BadRequestException("${e.message}")
            }
            shikimoriUsersRepository.save(
                ShikimoriUsers(
                    id = shikimoriWhoAmi.id,
                    image = shikimoriWhoAmi.avatar,
                    username = shikimoriWhoAmi.nickname
                )
            )
            response.status = HttpStatus.CREATED.value()
        }
        return RedirectView("https://anifox.club/")
    }

    fun makeCookieAniFox(
        response: HttpServletResponse,
        username: String,
        password: String
    ) {
        val auth = authzClient.obtainAccessToken(username, password)

        response.addHeader("refreshToken", auth.refreshToken)
        response.addHeader("accessToken", auth.token)
    }
}