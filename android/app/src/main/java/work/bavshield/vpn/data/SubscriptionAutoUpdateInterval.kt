package work.bavshield.vpn.data

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

    val logLabel: String
        get() = if (this == OFF) "off" else "${hours}h"

    companion object {
        val DEFAULT = SIX_HOURS

        fun fromStoredHours(hours: Int?): SubscriptionAutoUpdateInterval {
            if (hours == null) return DEFAULT
            return entries.find { it.hours == hours } ?: DEFAULT
        }
    }
}
