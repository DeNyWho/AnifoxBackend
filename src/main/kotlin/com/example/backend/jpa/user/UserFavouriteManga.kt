package com.example.backend.jpa.user

import com.example.backend.jpa.anime.AnimeTable
import com.example.backend.jpa.manga.MangaTable
import com.example.backend.models.users.StatusFavourite
import java.util.*
import javax.persistence.*


@Entity
@Table(name = "user_favorites_manga", schema = "users")
data class UserFavouriteManga(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL],)
    @JoinColumn(name = "manga_id")
    val manga: MangaTable = MangaTable(),

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: StatusFavourite = StatusFavourite.Watching
)