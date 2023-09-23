package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "studio", schema = "anime")
data class AnimeStudioTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
)
