package online.coffemaniavpn.client.data

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class SubscriptionInfo(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
) {
    val used: Long get() = (upload + download).coerceAtLeast(0)
    val isUnlimitedTraffic: Boolean get() = total <= 0
    val usageFraction: Float
        get() = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    fun trafficLabel(): String {
        val usedText = formatTrafficBytes(used)
        val totalText = if (isUnlimitedTraffic) "∞" else formatTrafficBytes(total)
        return "$usedText / $totalText"
    }
}

data class SubscriptionFetchResult(
    val nodes: List<ProxyNode>,
    val info: SubscriptionInfo?,
)

object SubscriptionInfoParser {
    fun parseHeader(raw: String): SubscriptionInfo? {
        val values = raw.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate { part ->
                val key = part.substringBefore('=').trim().lowercase(Locale.US)
                val value = part.substringAfter('=', "0").trim().toLongOrNull() ?: 0L
                key to value
            }

        if (values.isEmpty()) return null

        return SubscriptionInfo(
            upload = values["upload"] ?: 0L,
            download = values["download"] ?: 0L,
            total = values["total"] ?: 0L,
            expire = values["expire"] ?: 0L,
        )
    }

    fun parseFromBody(body: String): SubscriptionInfo? {
        return body.lineSequence()
            .map { it.trim().removePrefix("#").trim() }
            .firstOrNull { line ->
                line.contains("upload=", ignoreCase = true) &&
                    line.contains("download=", ignoreCase = true)
            }
            ?.let(::parseHeader)
    }
}

fun formatTrafficBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
