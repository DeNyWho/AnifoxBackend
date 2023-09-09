package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "anime_related", schema = "anime")
data class AnimeRelatedTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = true)
    val type: String? = "",

    @Column(nullable = true)
    val typeEn: String? = "",

    val shikimoriId: Int = 0,
)
