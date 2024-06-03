package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeVideoType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "video", schema = "anime")
data class AnimeVideoTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val imageUrl: String = "",
    val playerUrl: String = "",
    val name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    val type: AnimeVideoType,
)
