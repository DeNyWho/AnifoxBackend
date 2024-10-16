package club.anifox.backend.jpa.entity.anime

import club.anifox.backend.domain.enums.anime.AnimeExternalLinksType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "external_links", schema = "anime")
data class AnimeExternalLinksTable(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    var kind: AnimeExternalLinksType = AnimeExternalLinksType.OfficialSite,

    @Column(columnDefinition = "TEXT")
    val url: String,
)
