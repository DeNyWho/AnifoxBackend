package club.anifox.backend.jpa.entity.anime.common

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.util.*

@Entity
@BatchSize(size = 10)
@Table(name = "studio", schema = "anime")
data class AnimeStudioTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
)
