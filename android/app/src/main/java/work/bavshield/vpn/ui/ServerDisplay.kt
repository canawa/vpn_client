package work.bavshield.vpn.ui

import android.content.Context
import androidx.compose.runtime.Immutable
import work.bavshield.vpn.R
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ProxyNode
import java.text.Collator
import java.util.Locale

@Immutable
data class ServerDisplay(
    val flag: String,
    val title: String,
    val subtitle: String,
    val protocolLabel: String,
    val pingText: String,
    val pingMs: Int?,
    val isAutoSelect: Boolean = false,
    val isBypassGroup: Boolean = false,
)

object ServerDisplayMapper {
    /** Первый emoji-флаг в названии (нулевая позиция / первый токен до пробела). */
    private val firstFlagRegex = Regex("^(\\p{Regional_Indicator}{2}|\\p{Extended_Pictographic})")
    private val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val titleCollator: Collator = Collator.getInstance(Locale("ru", "RU")).apply {
        strength = Collator.PRIMARY
    }

    private fun isAutoSelect(name: String, title: String): Boolean {
        val haystack = "$name $title".lowercase()
        return haystack.contains("автовыбор") ||
            haystack.contains("autoselect") ||
            haystack.contains("auto select") ||
            haystack.contains("auto-select")
    }

    /** Обход БС / списки / special servers without a country flag. */
    private fun isBypassGroup(name: String, title: String, hasCountryFlag: Boolean): Boolean {
        val haystack = "$name $title".lowercase()
        if (haystack.contains("обход") ||
            haystack.contains("bypass") ||
            haystack.contains("списки") ||
            Regex("""(^|[^a-zа-яё])бс([^a-zа-яё]|$)""").containsMatchIn(haystack)
        ) {
            return true
        }
        return !hasCountryFlag
    }

    private fun isIpv4Address(value: String): Boolean {
        val host = value.substringBefore(':').trim()
        if (!ipv4Regex.matches(host)) return false
        return host.split('.').all { octet ->
            val n = octet.toIntOrNull() ?: return false
            n in 0..255
        }
    }

    /** Не показываем технический endpoint (IP или host:port узла) под названием сервера. */
    private fun isHiddenSubtitle(text: String, node: ProxyNode): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        if (trimmed == node.host || trimmed == "${node.host}:${node.port}") return true
        if (isIpv4Address(trimmed)) return true
        return false
    }

    fun map(context: Context, node: ProxyNode, ping: PingState? = null): ServerDisplay {
        val trimmed = node.name.trim()
        val parsedFlag = firstFlagRegex.find(trimmed)?.value
            ?: trimmed.substringBefore(' ').trim().takeIf { it.isNotEmpty() }
        val withoutFlag = trimmed.removePrefix(parsedFlag.orEmpty()).trim()
        val autoSelect = isAutoSelect(trimmed, withoutFlag.substringBefore("|").trim())
        val hasCountryFlag = !autoSelect && FlagUtils.emojiToCountryCode(parsedFlag.orEmpty()) != null
        val flag = when {
            autoSelect -> FlagUtils.PIRATE_FLAG
            hasCountryFlag -> parsedFlag!!
            else -> FlagUtils.PIRATE_FLAG
        }

        val title = withoutFlag.substringBefore("|").trim().let { candidate ->
            when {
                autoSelect -> context.getString(R.string.server_autoselect)
                candidate.isNotBlank() &&
                    !isIpv4Address(candidate) &&
                    candidate != "${node.host}:${node.port}" -> candidate
                isIpv4Address(node.host) -> context.getString(R.string.server_generic)
                node.host.isNotBlank() -> node.host
                else -> context.getString(R.string.server_generic)
            }
        }

        val subtitle = when {
            autoSelect -> ""
            else -> withoutFlag.substringAfter("|", "").trim().let { raw ->
                if (isHiddenSubtitle(raw, node)) "" else raw
            }
        }

        val (pingText, pingMs) = when (ping) {
            null -> "—" to null
            PingState.Loading -> "…" to null
            is PingState.Result -> "${ping.latencyMs} ms" to ping.latencyMs
            PingState.Unreachable -> "N/A" to null
        }

        return ServerDisplay(
            flag = flag,
            title = title,
            subtitle = subtitle,
            protocolLabel = node.protocolLabel(),
            pingText = pingText,
            pingMs = pingMs,
            isAutoSelect = autoSelect,
            isBypassGroup = !autoSelect && isBypassGroup(trimmed, title, hasCountryFlag),
        )
    }

    /**
     * Order: автовыбор → страны A–Я → обход БС / без страны.
     * Inside each group — alphabetical (ru).
     */
    fun <T> sortRows(rows: List<Pair<T, ServerDisplay>>): List<Pair<T, ServerDisplay>> =
        rows.sortedWith(
            compareBy<Pair<T, ServerDisplay>> { (_, display) ->
                when {
                    display.isAutoSelect -> 0
                    display.isBypassGroup -> 2
                    else -> 1
                }
            }.thenBy(titleCollator) { it.second.title }
                .thenBy(titleCollator) { it.second.subtitle },
        )
}
