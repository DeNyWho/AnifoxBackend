package club.anifox.backend.service.anime.components

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.jpa.entity.anime.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

@Component
class AnimeSearchComponent {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    private lateinit var animeGenreRepository: AnimeGenreRepository

    fun getAnimeSearch(
        pageNum: Int,
        pageSize: Int,
        genres: List<String>?,
        status: AnimeStatus?,
        filter: AnimeSearchFilter?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studio: String?,
    ): List<AnimeLight> {
        return findAnime(
            pageable = PageRequest.of(pageNum, pageSize),
            status = status,
            searchQuery = searchQuery,
            ratingMpa = ratingMpa,
            season = season,
            minimalAge = minimalAge,
            type = type,
            year = year,
            genres = genres,
            translationIds = translations,
            studio = studio,
            filter = filter,
        ).map {
            it.toAnimeLight()
        }
    }

    private fun findAnime(
        pageable: Pageable,
        status: AnimeStatus?,
        searchQuery: String?,
        ratingMpa: String?,
        season: AnimeSeason?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        genres: List<String>?,
        studio: String?,
        translationIds: List<String>?,
        filter: AnimeSearchFilter?,
    ): List<AnimeTable> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)
        criteriaQuery.select(root)

        val predicates: MutableList<Predicate> = mutableListOf()
        if (status != null) {
            predicates.add(criteriaBuilder.equal(root.get<String>("status"), AnimeStatus.valueOf(status.name)))
        }

        if (!ratingMpa.isNullOrEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get<String>("ratingMpa"), ratingMpa))
        }

        if (season != null) {
            predicates.add(criteriaBuilder.equal(root.get<String>("season"), AnimeSeason.valueOf(season.name)))
        }

        if (type != null) {
            predicates.add(criteriaBuilder.equal(root.get<String>("type"), AnimeType.valueOf(type.name)))
        }

        if (minimalAge != null) {
            predicates.add(criteriaBuilder.equal(root.get<Int>("minimalAge"), minimalAge))
        }

        if (!year.isNullOrEmpty()) {
            predicates.add(root.get<Int>("year").`in`(year))
        }

        if (!studio.isNullOrEmpty()) {
            val studioTable = animeStudiosRepository.findByStudio(studio)
                .orElseThrow { throw NotFoundException("Studio not found") }
            val studioPredicate = criteriaBuilder.isMember(studioTable, root.get<List<AnimeStudioTable>>("studios"))
            predicates.add(studioPredicate)
        }

        if (!genres.isNullOrEmpty()) {
            val g = mutableListOf<AnimeGenreTable>()
            genres.forEach {
                g.add(animeGenreRepository.findById(it).get())
            }
            for (genre in g) {
                val genrePredicate = criteriaBuilder.isMember(genre, root.get<List<AnimeGenreTable>>("genres"))
                predicates.add(genrePredicate)
            }
        }
        if (!searchQuery.isNullOrEmpty()) {
            val titleExpression: Expression<Boolean> = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%",
            )

            val exactMatchPredicate: Predicate = criteriaBuilder.equal(root.get<String>("title"), searchQuery)

            val otherTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("otherTitles", JoinType.LEFT)
            val otherTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(otherTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%",
            )

            val enTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("titleEn", JoinType.LEFT)
            val enTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(enTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%",
            )

            val japTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("titleJapan", JoinType.LEFT)
            val japTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(japTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%",
            )

            val synTitlesJoin: ListJoin<AnimeTable, String> = root.joinList("synonyms", JoinType.LEFT)
            val synTitlesExpression = criteriaBuilder.like(
                criteriaBuilder.lower(synTitlesJoin),
                "%" + searchQuery.lowercase(Locale.getDefault()) + "%",
            )

            predicates.addAll(
                listOf(
                    criteriaBuilder.or(titleExpression, exactMatchPredicate),
                    criteriaBuilder.or(otherTitlesExpression),
                    criteriaBuilder.or(enTitlesExpression),
                    criteriaBuilder.or(japTitlesExpression),
                    criteriaBuilder.or(synTitlesExpression),
                ),
            )
        }
        if (!translationIds.isNullOrEmpty()) {
            val translationJoin = root.join<AnimeTable, AnimeTranslationTable>("translations")

            val translationIdsPredicate = criteriaBuilder.isTrue(
                translationJoin.get<AnimeTranslationTable>("id").`in`(
                    translationIds.mapNotNull { it.toIntOrNull() }.toList(),
                ),
            )

            predicates.add(translationIdsPredicate)
        }
        if (predicates.isNotEmpty()) {
            if (searchQuery == null) {
                criteriaQuery.distinct(true).where(criteriaBuilder.and(*predicates.toTypedArray()))
            } else {
                criteriaQuery.distinct(true).where(criteriaBuilder.or(*predicates.toTypedArray()))
            }
        }

        val sort: List<Order> = when (filter) {
//            AnimeSearchFilter.Popular -> {
//                listOf(
//                    criteriaBuilder.desc(root.get<AnimeTable>("views")),
//                    criteriaBuilder.desc(root.get<AnimeTable>("countRate")),
//                )
//            }
            AnimeSearchFilter.DateASC -> {
                listOf(criteriaBuilder.asc(root.get<AnimeTable>("airedAt")))
            }
            AnimeSearchFilter.DateDESC -> {
                listOf(criteriaBuilder.desc(root.get<AnimeTable>("airedAt")))
            }
            AnimeSearchFilter.ShikimoriRating -> {
                listOf(criteriaBuilder.desc(root.get<AnimeTable>("shikimoriRating")))
            }
            AnimeSearchFilter.Random -> {
                listOf(criteriaBuilder.asc(criteriaBuilder.function("random", AnimeTable::class.java)))
            }
            else -> emptyList()
        }

        criteriaQuery.orderBy(sort)

        val query = entityManager.createQuery(criteriaQuery)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return query.resultList
    }
}
