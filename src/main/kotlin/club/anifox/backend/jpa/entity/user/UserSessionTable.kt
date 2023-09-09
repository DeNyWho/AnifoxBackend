package club.anifox.backend.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(schema = "users", name = "user_sessions")
data class UserSessionTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val sessionId: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val userTable: UserTable = UserTable(),

    @Column(nullable = false)
    val creationTime: LocalDateTime = LocalDateTime.now(),

    var lastAccessTime: LocalDateTime = LocalDateTime.now(),
)
