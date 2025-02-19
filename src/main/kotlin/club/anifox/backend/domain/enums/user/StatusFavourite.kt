package club.anifox.backend.domain.enums.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Anime favourite status")
enum class StatusFavourite(val jsonKey: String) {
    @Schema(description = "Plan to watch", example = "plan_to_watch")
    InPlan("plan_to_watch"),

    @Schema(description = "Currently watching", example = "watching")
    Watching("watching"),

    @Schema(description = "Finished watching", example = "completed")
    Completed("completed"),

    @Schema(description = "Dropped watching", example = "dropped")
    Dropped("dropped"),

    @Schema(description = "Put on hold", example = "on_hold")
    OnHold("on_hold");

    override fun toString(): String {
        return jsonKey
    }
}
