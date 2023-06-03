package com.example.backend.models.users

import com.example.backend.jpa.user.Role
import kotlinx.serialization.Serializable

@Serializable
data class WhoAmi(
    val username: String = "",
    val email: String? = null,
    val nickName: String? = null,
    var typeUser: TypeUser = TypeUser.AniFox,
    val roles: MutableSet<Role> = mutableSetOf(),
    val image: String? = null
)