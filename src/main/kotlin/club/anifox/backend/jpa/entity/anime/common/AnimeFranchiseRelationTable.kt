package club.anifox.backend.jpa.entity.anime.common

import club.anifox.backend.domain.enums.anime.AnimeRelationFranchise
import club.anifox.backend.jpa.entity.anime.AnimeTable
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
@Table(name = "franchise_relation", schema = "anime")
class AnimeFranchiseRelationTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franchise_url", referencedColumnName = "url", nullable = false)
    val franchise: AnimeFranchiseTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    val source: AnimeTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    val target: AnimeTable,

    @Enumerated(EnumType.STRING)
    @Column(length = 100, nullable = false)
    val relationType: AnimeRelationFranchise,

    @Column(length = 100, nullable = false)
    val relationTypeRus: String,
)
