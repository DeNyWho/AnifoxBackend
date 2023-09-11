package club.anifox.backend.infrastructure.persistence.jpa.entity.anime

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "genres", schema = "anime")
data class AnimeGenreTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val genre: String = "",
)
