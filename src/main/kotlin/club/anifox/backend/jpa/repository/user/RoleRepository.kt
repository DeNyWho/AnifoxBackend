package club.anifox.backend.jpa.repository.user

import club.anifox.backend.domain.model.enums.user.RoleName
import club.anifox.backend.jpa.entity.user.RoleTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RoleRepository : JpaRepository<RoleTable, String> {
    fun findByName(name: RoleName): Optional<RoleTable>
}
