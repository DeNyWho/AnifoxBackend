package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "studios", schema = "anime")
data class AnimeStudiosTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val studio: String = "",
)
