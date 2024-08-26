package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "franchise", schema = "anime")
class AnimeFranchiseTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    val title: String = UUID.randomUUID().toString(),

    val urlPath: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    val source: AnimeTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    val target: AnimeTable,

    @Enumerated(EnumType.STRING)
    @Column(length = 100, nullable = false)
    val relationType: AnimeRelationFranchise,

    @Column(length = 100, nullable = false)
    val relationTypeRus: String,
)
