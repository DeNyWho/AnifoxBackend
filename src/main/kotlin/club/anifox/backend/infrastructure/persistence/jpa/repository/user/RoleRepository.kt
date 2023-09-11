package club.anifox.backend.infrastructure.persistence.jpa.repository.user

import club.anifox.backend.domain.enums.user.RoleName
import club.anifox.backend.infrastructure.persistence.jpa.entity.user.RoleTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RoleRepository : JpaRepository<RoleTable, String> {
    fun findByName(name: RoleName): Optional<RoleTable>
}
