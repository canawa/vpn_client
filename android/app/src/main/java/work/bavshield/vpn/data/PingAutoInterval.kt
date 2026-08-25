package work.bavshield.vpn.data

enum class PingAutoInterval(val minutes: Int) {
    OFF(0),
    TEN(10),
    TWELVE(12),
    FIFTEEN(15),
    ;

    val durationMs: Long
        get() = minutes * 60_000L

    val logLabel: String
        get() = if (this == OFF) "off" else "${minutes}m"

    companion object {
        val DEFAULT = TWELVE

        fun fromStoredMinutes(minutes: Int?): PingAutoInterval {
            if (minutes == null) return DEFAULT
            return entries.find { it.minutes == minutes } ?: DEFAULT
        }
    }
}
