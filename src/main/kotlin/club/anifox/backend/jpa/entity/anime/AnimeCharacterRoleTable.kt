package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "anime_character_role",
    schema = "anime",
    uniqueConstraints = [UniqueConstraint(columnNames = ["anime_id", "character_id"])],
)
data class AnimeCharacterRoleTable(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    val anime: AnimeTable = AnimeTable(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    val character: AnimeCharacterTable = AnimeCharacterTable(),

    @Column(nullable = false)
    val role: String = "",

    @Column(nullable = false)
    val roleEn: String = "",
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeCharacterRoleTable) return false
        return id == other.id
    }
}
