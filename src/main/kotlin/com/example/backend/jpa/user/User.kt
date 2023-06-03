package com.example.backend.jpa.user

import com.example.backend.jpa.anime.AnimeRating
import com.example.backend.models.users.TypeUser
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "users", schema = "users")
data class User(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val username: String = "",

    @Column(nullable = true)
    val password: String? = null,

    @Column(nullable = true, unique = true)
    val email: String? = null,

    @Column(nullable = true)
    val image: String? = null,

    @Column(nullable = true)
    val nickName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = true)
    var typeUser: TypeUser = TypeUser.AniFox,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        schema = "users",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<Role> = mutableSetOf(),

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val favorites: MutableSet<UserFavoriteAnime> = mutableSetOf(),

    @OneToMany(
        mappedBy = "anime",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val rating: MutableSet<AnimeRating> = mutableSetOf()
)