package club.anifox.backend.jpa.entity.anime.common

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "franchise", schema = "anime")
class AnimeFranchiseTable(
    @Id
    val url: String = "",
    val title: String = "",
    @Column(columnDefinition = "TEXT")
    val description: String = "",
    @OneToMany(
        mappedBy = "franchise",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    val relations: List<AnimeFranchiseRelationTable> = mutableListOf(),
)
