package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Basic
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.util.*

@Entity
@Table(name = "character", schema = "anime")
data class AnimeCharacterTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(unique = true)
    val malId: Int = 0,
    @Column(columnDefinition = "TEXT")
    val name: String = "",
    @Column(columnDefinition = "TEXT")
    val nameEn: String = "",
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    val nameKanji: String? = null,
    val image: String = "",
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    val aboutEn: String? = null,
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    val aboutRu: String? = null,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "character_pictures", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 20)
    val pictures: MutableList<String> = mutableListOf(),
    @OneToMany(
        mappedBy = "character",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    @BatchSize(size = 20)
    val characterRoles: MutableSet<AnimeCharacterRoleTable> = mutableSetOf(),
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeCharacterTable) return false
        return id == other.id
    }
}
