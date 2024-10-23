package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "parser", schema = "anime")
data class AnimeErrorParserTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT")
    val cause: String? = null,
    val shikimoriId: Int = 0,
)
