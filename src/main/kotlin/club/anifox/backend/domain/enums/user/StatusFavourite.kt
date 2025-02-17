package club.anifox.backend.domain.enums.user

enum class StatusFavourite(val jsonKey: String) {
    InPlan("plan_to_watch"),
    Watching("watching"),
    Completed("completed"),
    Dropped("dropped"),
    OnHold("on_hold"),
    ;

    override fun toString(): String {
        return jsonKey
    }
}
