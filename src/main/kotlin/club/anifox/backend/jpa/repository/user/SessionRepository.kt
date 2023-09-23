package club.anifox.backend.jpa.repository.user

import club.anifox.backend.jpa.entity.user.UserSessionTable
import club.anifox.backend.jpa.entity.user.UserTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionRepository : JpaRepository<UserSessionTable, String> {
    fun findByUserTable(user: UserTable): List<UserSessionTable>
    fun findBySessionId(sessionId: String): UserSessionTable
}
