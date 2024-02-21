package club.anifox.backend.jpa.entity.anime.episodes

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "episodes_latest", schema = "anime")
data class AnimeLatestEpisodes(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val date: LocalDateTime,
)
