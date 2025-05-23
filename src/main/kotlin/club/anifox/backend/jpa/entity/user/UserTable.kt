package club.anifox.backend.jpa.entity.user

import club.anifox.backend.jpa.entity.anime.AnimeRatingTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "users", schema = "users")
data class UserTable(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    val login: String = "",
    @Column(nullable = true)
    var image: String? = null,
    @Column(nullable = true)
    var birthday: LocalDate? = null,
    @Column(nullable = true)
    var nickName: String? = null,
    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL])
    var preferredGenres: MutableSet<AnimeGenreTable> = mutableSetOf(),
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val favoritesAnime: MutableSet<UserFavoriteAnimeTable> = mutableSetOf(),
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val rating: MutableSet<AnimeRatingTable> = mutableSetOf(),
) {
    fun addPreferredGenres(genres: List<AnimeGenreTable>): UserTable {
        preferredGenres.addAll(genres)
        return this
    }

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserTable) return false
        return id == other.id
    }
}
