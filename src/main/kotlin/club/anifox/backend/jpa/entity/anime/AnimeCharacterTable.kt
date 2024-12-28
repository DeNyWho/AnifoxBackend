package club.anifox.backend.jpa.entity.anime

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
import java.util.UUID

@Entity
@Table(name = "character", schema = "anime")
data class AnimeCharacterTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val malId: Int = 0,
    @Column(columnDefinition = "TEXT")
    val name: String = "",
    @Column(columnDefinition = "TEXT")
    val nameEn: String = "",
    @Column(columnDefinition = "TEXT")
    val nameKanji: String = "",
    val image: String = "",
    @Column(columnDefinition = "TEXT", nullable = true)
    val aboutEn: String? = null,
    @Column(columnDefinition = "TEXT", nullable = true)
    val aboutRu: String? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_pictures", schema = "anime")
    @Column(columnDefinition = "text")
    @BatchSize(size = 10)
    val pictures: MutableList<String> = mutableListOf(),
    @OneToMany(
        mappedBy = "character",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val characterRoles: MutableSet<AnimeCharacterRoleTable> = mutableSetOf(),
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeCharacterTable) return false
        return id == other.id
    }
}
