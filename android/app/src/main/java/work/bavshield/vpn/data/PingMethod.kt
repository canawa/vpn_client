package work.bavshield.vpn.data

enum class PingMethod {
    TCP,
    HTTP_GET,
    ;

    val logLabel: String
        get() = when (this) {
            TCP -> "tcp"
            HTTP_GET -> "http_get"
        }

    companion object {
        val DEFAULT = TCP

        fun fromStored(raw: String?): PingMethod {
            if (raw.isNullOrBlank()) return DEFAULT
            return entries.find { it.name.equals(raw, ignoreCase = true) } ?: DEFAULT
        }
    }
}
