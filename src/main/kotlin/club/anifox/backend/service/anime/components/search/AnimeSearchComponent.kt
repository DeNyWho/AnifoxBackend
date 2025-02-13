package club.anifox.backend.service.anime.components.search

import club.anifox.backend.domain.enums.anime.AnimeSeason
import club.anifox.backend.domain.enums.anime.AnimeStatus
import club.anifox.backend.domain.enums.anime.AnimeType
import club.anifox.backend.domain.enums.anime.filter.AnimeDefaultFilter
import club.anifox.backend.domain.enums.anime.filter.AnimeSearchFilter
import club.anifox.backend.domain.enums.common.LanguageType.*
import club.anifox.backend.domain.mappers.anime.light.toAnimeLight
import club.anifox.backend.domain.model.anime.light.AnimeLight
import club.anifox.backend.jpa.entity.anime.AnimeTable
import club.anifox.backend.jpa.entity.anime.common.AnimeGenreTable
import club.anifox.backend.jpa.entity.anime.common.AnimeImagesTable
import club.anifox.backend.jpa.entity.anime.common.AnimeStudioTable
import club.anifox.backend.jpa.entity.anime.episodes.AnimeTranslationTable
import club.anifox.backend.jpa.repository.anime.AnimeGenreRepository
import club.anifox.backend.jpa.repository.anime.AnimeStudiosRepository
import club.anifox.backend.util.detectLanguage
import club.anifox.backend.util.replaceLast
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AnimeSearchComponent {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var animeStudiosRepository: AnimeStudiosRepository

    @Autowired
    private lateinit var animeGenreRepository: AnimeGenreRepository

    @Transactional(readOnly = true)
    fun getAnimeSearch(
        page: Int,
        limit: Int,
        genres: List<String>?,
        status: AnimeStatus?,
        orderBy: AnimeSearchFilter?,
        sort: AnimeDefaultFilter?,
        searchQuery: String?,
        season: AnimeSeason?,
        ratingMpa: String?,
        minimalAge: Int?,
        type: AnimeType?,
        year: List<Int>?,
        translations: List<String>?,
        studios: List<String>?,
        episodeCount: Int?,
    ): List<AnimeLight> {
        return findAnime(
            pageable = PageRequest.of(page, limit),
            status = status,
            searchQuery = searchQuery,
            ratingMpa = ratingMpa,
            season = season,
            minimalAge = minimalAge,
            type = type,
            year = year,
            genres = genres,
            translationIds = translations,
            studios = studios,
            orderBy = orderBy,
            sort = sort,
            episodeCount = episodeCount,
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
        studios: List<String>?,
        translationIds: List<String>?,
        orderBy: AnimeSearchFilter?,
        sort: AnimeDefaultFilter?,
        episodeCount: Int?,
    ): List<AnimeTable> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(AnimeTable::class.java)
        val root = criteriaQuery.from(AnimeTable::class.java)
        criteriaQuery.select(root)

        root.fetch<AnimeGenreTable, Any>("genres", JoinType.LEFT)
        root.fetch<AnimeImagesTable, Any>("images", JoinType.LEFT)
        root.fetch<AnimeStudioTable, Any>("studios", JoinType.LEFT)

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

        if (episodeCount != null) {
            predicates.add(criteriaBuilder.equal(root.get<Int>("episodesCount"), episodeCount))
        }

        if (!year.isNullOrEmpty()) {
            if (year.size == 1) {
                predicates.add(criteriaBuilder.equal(root.get<Int>("year"), year[0]))
            } else if (year.size == 2) {
                predicates.add(
                    criteriaBuilder.between(root.get("year"), year[0], year[1]),
                )
            }
        }

        if (!studios.isNullOrEmpty()) {
            val s = mutableListOf<AnimeStudioTable>()
            studios.forEach {
                s.add(animeStudiosRepository.findById(it).get())
            }
            for (studio in s) {
                val studioPredicate = criteriaBuilder.isMember(studio, root.get<List<AnimeStudioTable>>("studios"))
                predicates.add(studioPredicate)
            }
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
            // Нормализуем поисковый запрос и создаем варианты написания
            val normalizedSearchQuery = normalizeText(searchQuery)
            val searchVariants = generateSearchVariants(normalizedSearchQuery)

            println("FVCX = $searchVariants")

            val language = searchQuery.detectLanguage()

            val searchFields = mutableListOf(
                root.get<String>("title"),
            )

            when (language) {
                JAPANESE -> {
                    searchFields.add(root.joinList<AnimeTable, String>("titleJapan", JoinType.LEFT))
                }
                RUSSIAN -> {
                    searchFields.add(root.joinList<AnimeTable, String>("titleOther", JoinType.LEFT))
                    searchFields.add(root.joinList<AnimeTable, String>("synonyms", JoinType.LEFT))
                }
                ENGLISH -> {
                    searchFields.add(root.joinList<AnimeTable, String>("titleEn", JoinType.LEFT))
                }
                UNKNOWN -> { }
            }

            val searchPredicates = mutableListOf<Predicate>()

            // Для каждого варианта написания создаем предикаты
            searchVariants.forEach { variant ->
                searchFields.forEach { field ->
                    // Точное совпадение (case-insensitive)
                    searchPredicates.add(
                        criteriaBuilder.equal(
                            criteriaBuilder.function(
                                "LOWER",
                                String::class.java,
                                field,
                            ),
                            variant.lowercase(),
                        ),
                    )

                    // Совпадение без специальных символов
                    searchPredicates.add(
                        criteriaBuilder.equal(
                            criteriaBuilder.function(
                                "REPLACE",
                                String::class.java,
                                criteriaBuilder.function(
                                    "REPLACE",
                                    String::class.java,
                                    criteriaBuilder.function(
                                        "LOWER",
                                        String::class.java,
                                        field,
                                    ),
                                    criteriaBuilder.literal("-"),
                                    criteriaBuilder.literal(""),
                                ),
                                criteriaBuilder.literal(" "),
                                criteriaBuilder.literal(""),
                            ),
                            variant.replace("-", "").replace(" ", "").lowercase(),
                        ),
                    )

                    // Частичное совпадение
                    searchPredicates.add(
                        criteriaBuilder.like(
                            criteriaBuilder.function(
                                "LOWER",
                                String::class.java,
                                field,
                            ),
                            "%${variant.lowercase()}%",
                        ),
                    )
                }
            }

            // Создаем предикаты для слитного написания
            val concatenatedQuery = normalizedSearchQuery.replace("-", "").replace(" ", "")
            searchFields.forEach { field ->
                searchPredicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.function(
                            "REPLACE",
                            String::class.java,
                            criteriaBuilder.function(
                                "REPLACE",
                                String::class.java,
                                criteriaBuilder.function(
                                    "LOWER",
                                    String::class.java,
                                    field,
                                ),
                                criteriaBuilder.literal("-"),
                                criteriaBuilder.literal(""),
                            ),
                            criteriaBuilder.literal(" "),
                            criteriaBuilder.literal(""),
                        ),
                        "%${concatenatedQuery.lowercase()}%",
                    ),
                )
            }

            // Объединяем предикаты
            predicates.add(
                criteriaBuilder.or(*searchPredicates.toTypedArray()),
            )
        }

        if (!translationIds.isNullOrEmpty()) {
            val translationJoin = root.join<AnimeTable, AnimeTranslationTable>("translations")

            val translationIdsPredicate =
                criteriaBuilder.isTrue(
                    translationJoin.get<AnimeTranslationTable>("id").`in`(
                        translationIds.mapNotNull { it.toIntOrNull() }.toList(),
                    ),
                )

            predicates.add(translationIdsPredicate)
        }

        val sortOrder: List<Order> =
            when (orderBy) {
                AnimeSearchFilter.Update -> {
                    predicates.add(criteriaBuilder.isNotNull(root.get<AnimeTable>("updatedAt")))

                    if (sort == AnimeDefaultFilter.Asc) {
                        listOf(criteriaBuilder.asc(root.get<AnimeTable>("updatedAt")))
                    } else {
                        listOf(criteriaBuilder.desc(root.get<AnimeTable>("updatedAt")))
                    }
                }
                AnimeSearchFilter.Aired -> {
                    predicates.add(criteriaBuilder.isNotNull(root.get<AnimeTable>("airedOn")))

                    if (sort == AnimeDefaultFilter.Asc) {
                        listOf(criteriaBuilder.asc(root.get<AnimeTable>("airedOn")))
                    } else {
                        listOf(criteriaBuilder.desc(root.get<AnimeTable>("airedOn")))
                    }
                }
                AnimeSearchFilter.Released -> {
                    predicates.add(criteriaBuilder.isNotNull(root.get<AnimeTable>("releasedOn")))

                    if (sort == AnimeDefaultFilter.Asc) {
                        listOf(criteriaBuilder.asc(root.get<AnimeTable>("releasedOn")))
                    } else {
                        listOf(criteriaBuilder.desc(root.get<AnimeTable>("releasedOn")))
                    }
                }
                AnimeSearchFilter.Rating -> {
                    val votesOrder =
                        if (sort == AnimeDefaultFilter.Asc) {
                            criteriaBuilder.asc(root.get<Double>("shikimoriVotes"))
                        } else {
                            criteriaBuilder.desc(root.get<Double>("shikimoriVotes"))
                        }

                    val ratingOrder =
                        if (sort == AnimeDefaultFilter.Asc) {
                            criteriaBuilder.asc(root.get<Double>("shikimoriRating"))
                        } else {
                            criteriaBuilder.desc(root.get<Double>("shikimoriRating"))
                        }

                    listOf(ratingOrder, votesOrder)
                }
                AnimeSearchFilter.Random -> {
                    listOf(criteriaBuilder.asc(criteriaBuilder.function("RANDOM", Double::class.java)))
                }
                else -> emptyList()
            }

        if (predicates.isNotEmpty()) {
            if (searchQuery == null) {
                criteriaQuery.distinct(true).where(criteriaBuilder.and(*predicates.toTypedArray()))
            } else {
                criteriaQuery.distinct(true).where(criteriaBuilder.or(*predicates.toTypedArray()))
            }
        }

        criteriaQuery.orderBy(sortOrder)

        val query = entityManager.createQuery(criteriaQuery)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize

        return query.resultList
    }

    private fun normalizeText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun generateSearchVariants(text: String): MutableList<String> {
        val variants = mutableListOf<String>()

        variants.add(text)

        if (text.contains("ё")) {
            variants.add(text.replace("ё", "е"))
        }

        if (text.contains("е")) {
            variants.add(text.replace("е".toRegex(), "ё"))
        }

        if (text.contains("ё")) {
            variants.add(text.replaceFirst("ё", "е"))
        }

        if (text.contains("е")) {
            variants.add(text.replaceFirst("е", "ё"))
        }

        if (text.contains("ё")) {
            variants.add(text.replaceLast("ё", "е"))
        }

        if (text.contains("е")) {
            variants.add(text.replaceLast("е", "ё"))
        }

        if (text.contains(" ")) {
            variants.add(text.replace(" ", "-"))
        }

        if (text.contains("-")) {
            variants.add(text.replace("-", " "))
        }

        variants.add(text.replace("-", "").replace(" ", ""))

        return variants
    }
}
