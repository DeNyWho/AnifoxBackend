package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeBlockedType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blocked_by_studio", schema = "anime")
data class AnimeBlockedByStudioTable(
    @Id
    val shikimoriStudioId: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var type: AnimeBlockedType = AnimeBlockedType.ALL,
)
