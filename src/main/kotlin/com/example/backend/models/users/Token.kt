package com.example.backend.models.users

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Token(
    @SerialName("exp") var exp: Int? = null,
    @SerialName("iat") var iat: Int? = null,
    @SerialName("jti") var jti: String? = null,
    @SerialName("iss") var iss: String? = null,
    @SerialName("aud") var aud: String? = null,
    @SerialName("sub") var sub: String? = null,
    @SerialName("typ") var typ: String? = null,
    @SerialName("azp") var azp: String? = null,
    @Transient @SerialName("session_state") var session_state: String? = null,
    @SerialName("acr") var acr: String? = null,
    @SerialName("allowed-origins") var allowedOrigins: List<String> = emptyList(),
    @SerialName("realm_access") var realmAccess: RealmAccess? = RealmAccess(),
    @SerialName("resource_access") var resourceAccess: ResourceAccess? = ResourceAccess(),
    @SerialName("scope") var scope: String? = null,
    @SerialName("sid") var sid: String? = null,
    @SerialName("email_verified") var emailVerified: Boolean? = null,
    @SerialName("preferred_username") var preferredUsername: String? = null,
    @SerialName("email") var email: String? = null
)

@Serializable
data class RealmAccess(
    @SerialName("roles") var roles: List<String> = emptyList()
)

@Serializable
data class ResourceAccess(
    @SerialName("account") var account: Account? = Account()
)

@Serializable
data class Account(
    @SerialName("roles") var roles: List<String> = emptyList()
)