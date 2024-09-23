package club.anifox.backend.jpa.repository.user

import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserTable, String> {
    override fun findById(id: String): Optional<UserTable>
}
