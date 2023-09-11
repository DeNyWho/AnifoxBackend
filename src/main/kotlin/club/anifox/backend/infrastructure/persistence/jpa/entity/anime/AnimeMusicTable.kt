package club.anifox.backend.infrastructure.persistence.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeMusicType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "music", schema = "anime")
data class AnimeMusicTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val name: String = "",
    val episodes: String = "",
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = true)
    var type: AnimeMusicType = AnimeMusicType.Opening,
    val hosting: String = "",
)
