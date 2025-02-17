package club.anifox.backend.jpa.entity.anime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "anime_rating_distribution", schema = "anime")
class AnimeRatingDistributionTable(
    @Id
    @MapsId
    @OneToOne
    @JoinColumn(name = "anime_id", nullable = false)
    val anime: AnimeTable,

    @Column(nullable = false, name = "score_one")
    var score1Count: Int = 0,

    @Column(nullable = false, name = "score_two")
    var score2Count: Int = 0,

    @Column(nullable = false, name = "score_three")
    var score3Count: Int = 0,

    @Column(nullable = false, name = "score_four")
    var score4Count: Int = 0,

    @Column(nullable = false, name = "score_five")
    var score5Count: Int = 0,

    @Column(nullable = false, name = "score_six")
    var score6Count: Int = 0,

    @Column(nullable = false, name = "score_seven")
    var score7Count: Int = 0,

    @Column(nullable = false, name = "score_eight")
    var score8Count: Int = 0,

    @Column(nullable = false, name = "score_nine")
    var score9Count: Int = 0,

    @Column(nullable = false, name = "score_ten")
    var score10Count: Int = 0,

    @Column(nullable = false, name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeRatingDistributionTable) return false
        return anime.id == other.anime.id
    }

    override fun hashCode(): Int {
        return anime.id.hashCode()
    }

    override fun toString(): String {
        return "AnimeRatingDistributionTable(animeId=${anime.id}, " +
            "scores=[1:$score1Count, 2:$score2Count, " +
            "3:$score3Count, 4:$score4Count, 5:$score5Count, " +
            "6:$score6Count, 7:$score7Count, 8:$score8Count, " +
            "9:$score9Count, 10:$score10Count], " +
            "updated=$updatedAt)"
    }

    fun getScoreCount(score: Int): Int {
        return when (score) {
            1 -> score1Count
            2 -> score2Count
            3 -> score3Count
            4 -> score4Count
            5 -> score5Count
            6 -> score6Count
            7 -> score7Count
            8 -> score8Count
            9 -> score9Count
            10 -> score10Count
            else -> 0
        }
    }
}
