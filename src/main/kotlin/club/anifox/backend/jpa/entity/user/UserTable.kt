package club.anifox.backend.jpa.entity.user

import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeRatingTable
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "users", schema = "users")
data class UserTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val login: String = "",

    @Column(nullable = true, unique = true)
    val email: String? = null,

    @Column(nullable = true)
    var image: String? = null,

    @Column(nullable = true)
    var nickName: String? = null,

    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.ALL],
    )
    var preferredGenres: MutableSet<AnimeGenreTable> = mutableSetOf(),

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val favoritesAnime: MutableSet<UserFavoriteAnimeTable> = mutableSetOf(),

    @OneToMany(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val rating: MutableSet<AnimeRatingTable> = mutableSetOf(),
) {
    fun addPreferredGenres(genres: List<AnimeGenreTable>): UserTable {
        preferredGenres.addAll(genres)
        return this
    }
}
