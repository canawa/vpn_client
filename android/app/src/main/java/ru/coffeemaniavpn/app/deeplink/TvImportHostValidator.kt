package ru.coffeemaniavpn.app.deeplink

/**
 * Разрешённые адреса TV для POST (ТЗ §7).
 * Только IPv4; hostname / IPv6 / публичные IP — отказ.
 */
object TvImportHostValidator {
    fun isAllowed(host: String): Boolean {
        val trimmed = host.trim()
        if (trimmed.isEmpty() || trimmed.contains(':')) return false
        if (trimmed.any { it.isLetter() }) return false

        val parts = trimmed.split('.')
        if (parts.size != 4) return false
        val octets = IntArray(4)
        for (i in 0..3) {
            val n = parts[i].toIntOrNull() ?: return false
            if (n !in 0..255) return false
            octets[i] = n
        }
        if (octets[0] == 0 && octets[1] == 0 && octets[2] == 0 && octets[3] == 0) {
            return false
        }
        // 127.0.0.1 — отладка
        if (octets[0] == 127 && octets[1] == 0 && octets[2] == 0 && octets[3] == 1) {
            return true
        }
        // 10.0.0.0/8
        if (octets[0] == 10) return true
        // 172.16.0.0/12
        if (octets[0] == 172 && octets[1] in 16..31) return true
        // 192.168.0.0/16
        if (octets[0] == 192 && octets[1] == 168) return true
        return false
    }
}
