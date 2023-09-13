package club.anifox.backend.jpa.repository.user

import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserTable, String> {
    fun findByUsername(username: String): Optional<UserTable>
    fun findByEmail(email: String): Optional<UserTable>
    fun findByUsernameOrEmail(username: String, email: String): Optional<UserTable>
}
