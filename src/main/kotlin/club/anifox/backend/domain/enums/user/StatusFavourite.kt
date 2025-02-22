package club.anifox.backend.domain.enums.user

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Anime favourite status")
enum class StatusFavourite(val jsonKey: String) {
    @SerialName("plan_to_watch")
    @Schema(description = "Plan to watch", example = "plan_to_watch")
    InPlan("plan_to_watch"),

    @SerialName("watching")
    @Schema(description = "Currently watching", example = "watching")
    Watching("watching"),

    @SerialName("completed")
    @Schema(description = "Finished watching", example = "completed")
    Completed("completed"),

    @SerialName("dropped")
    @Schema(description = "Dropped watching", example = "dropped")
    Dropped("dropped"),

    @SerialName("on_hold")
    @Schema(description = "Put on hold", example = "on_hold")
    OnHold("on_hold"),
    ;

    override fun toString(): String {
        return jsonKey
    }
}
