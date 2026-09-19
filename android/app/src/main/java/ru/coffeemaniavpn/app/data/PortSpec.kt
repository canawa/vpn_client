package ru.coffeemaniavpn.app.data

/**
 * Порт из HS2/Hysteria URI и JSON, включая multi-port / hop:
 * `443`, `1234,5678`, `20000-50000`, `1234,5000-6000`, sing-box `2080:3000`.
 * Для клиента берём первый валидный порт.
 */
object PortSpec {
    fun firstPort(raw: Any?, default: Int = 443): Int {
        return when (raw) {
            null -> default
            is Number -> raw.toInt().takeIf { it in 1..65535 } ?: default
            is String -> firstPortFromText(raw, default)
            else -> firstPortFromText(raw.toString(), default)
        }
    }

    fun firstPortFromText(raw: String, default: Int = 443): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return default
        val firstSegment = trimmed.substringBefore(',').trim()
        val start = firstSegment.split('-', ':').firstOrNull()?.trim().orEmpty()
        return start.toIntOrNull()?.takeIf { it in 1..65535 } ?: default
    }
}
