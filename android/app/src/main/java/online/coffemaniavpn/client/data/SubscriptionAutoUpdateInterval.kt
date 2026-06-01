package online.coffemaniavpn.client.data

enum class SubscriptionAutoUpdateInterval(val hours: Int) {
    OFF(0),
    ONE_HOUR(1),
    THREE_HOURS(3),
    SIX_HOURS(6),
    TWELVE_HOURS(12),
    TWENTY_FOUR_HOURS(24),
    ;

    val durationMs: Long
        get() = hours * 3_600_000L

    val label: String
        get() = when (this) {
            OFF -> "Выкл"
            ONE_HOUR -> "1 ч"
            THREE_HOURS -> "3 ч"
            SIX_HOURS -> "6 ч"
            TWELVE_HOURS -> "12 ч"
            TWENTY_FOUR_HOURS -> "24 ч"
        }

    companion object {
        val DEFAULT = SIX_HOURS

        fun fromStoredHours(hours: Int?): SubscriptionAutoUpdateInterval {
            if (hours == null) return DEFAULT
            return entries.find { it.hours == hours } ?: DEFAULT
        }
    }
}
