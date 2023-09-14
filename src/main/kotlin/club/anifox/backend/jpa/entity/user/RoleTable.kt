package club.anifox.backend.jpa.entity.user

import club.anifox.backend.domain.enums.user.RoleName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "roles", schema = "users")
data class RoleTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val name: RoleName = RoleName.ROLE_USER,
)
