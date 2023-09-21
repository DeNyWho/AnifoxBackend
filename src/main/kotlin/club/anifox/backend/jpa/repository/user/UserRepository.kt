package club.anifox.backend.jpa.repository.user

import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserTable, String> {
    fun findByLogin(login: String): Optional<UserTable>

    fun findByEmail(email: String): Optional<UserTable>

    @Query("Select u from UserTable u where u.email = :userIdentifier or u.login = :userIdentifier")
    fun findByUsernameOrEmail(userIdentifier: String): Optional<UserTable>
}
