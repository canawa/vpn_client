package ru.coffeemaniavpn.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RoutingRuleMatcher {
    DomainSuffix,
    IpCidr,
}

@Serializable
enum class RoutingRuleTarget {
    Direct,
    Proxy,
    Block,
}

@Serializable
data class RoutingRule(
    val id: String = UUID.randomUUID().toString(),
    val value: String,
    val matcher: RoutingRuleMatcher,
    val target: RoutingRuleTarget,
    val isEnabled: Boolean = true,
)

object RoutingRuleInput {
    private val DomainRegex = Regex(
        """^(\*\.)?([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,}$""",
    )

    fun normalizeWebsite(raw: String): String {
        var value = raw.trim()
        if (value.isBlank()) return ""

        value = stripPrefixIgnoreCase(value, "https://")
        value = stripPrefixIgnoreCase(value, "http://")
        value = value.substringBefore('/')
        value = value.substringBefore('?')
        value = value.substringBefore('#')
        if (value.startsWith("[")) {
            value = value.removePrefix("[").removeSuffix("]")
        }
        value = value.substringBefore(':')

        return value.lowercase()
            .removePrefix("www.")
            .removePrefix(".")
            .trim()
    }

    fun looksLikeCidr(value: String): Boolean {
        val trimmed = value.trim()
        val slash = trimmed.indexOf('/')
        if (slash <= 0) return false
        val left = trimmed.substring(0, slash)
        val prefix = trimmed.substring(slash + 1).toIntOrNull() ?: return false
        if (prefix !in 0..128) return false
        return left.contains('.') || left.contains(':')
    }

    fun looksLikeDomain(value: String): Boolean {
        val trimmed = value.trim().lowercase()
        if (trimmed.isBlank()) return false
        if (trimmed.any { it.isWhitespace() }) return false
        if (trimmed.contains("://")) return false
        return DomainRegex.matches(trimmed)
    }

    fun classifyLine(raw: String): RoutingRuleLineKind {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return RoutingRuleLineKind.Blank
        if (looksLikeCidr(trimmed)) return RoutingRuleLineKind.Cidr
        if (looksLikeDomain(trimmed)) return RoutingRuleLineKind.Domain
        return RoutingRuleLineKind.Invalid
    }

    fun parseDraft(text: String): RoutingRuleDraftAnalysis {
        if (text.isEmpty()) {
            return RoutingRuleDraftAnalysis(emptyList())
        }
        val lines = mutableListOf<RoutingRuleDraftLine>()
        var offset = 0
        var number = 1
        while (true) {
            val nl = text.indexOf('\n', offset)
            val end = if (nl < 0) text.length else nl
            val part = text.substring(offset, end)
            lines += RoutingRuleDraftLine(
                number = number,
                raw = part,
                start = offset,
                end = end,
                kind = classifyLine(part),
            )
            if (nl < 0) break
            offset = nl + 1
            number += 1
        }
        return RoutingRuleDraftAnalysis(lines)
    }

    fun toStoredRule(raw: String, kind: RoutingRuleLineKind, target: RoutingRuleTarget): RoutingRule? {
        val trimmed = raw.trim()
        return when (kind) {
            RoutingRuleLineKind.Domain -> {
                val normalized = normalizeWebsite(trimmed.removePrefix("*."))
                if (normalized.isBlank()) null
                else RoutingRule(
                    value = normalized,
                    matcher = RoutingRuleMatcher.DomainSuffix,
                    target = target,
                )
            }
            RoutingRuleLineKind.Cidr -> RoutingRule(
                value = trimmed,
                matcher = RoutingRuleMatcher.IpCidr,
                target = target,
            )
            else -> null
        }
    }

    private fun stripPrefixIgnoreCase(value: String, prefix: String): String =
        if (value.startsWith(prefix, ignoreCase = true)) {
            value.substring(prefix.length)
        } else {
            value
        }
}

enum class RoutingRuleLineKind {
    Blank,
    Domain,
    Cidr,
    Invalid,
}

data class RoutingRuleDraftLine(
    val number: Int,
    val raw: String,
    val start: Int,
    val end: Int,
    val kind: RoutingRuleLineKind,
)

data class RoutingRuleDraftAnalysis(
    val lines: List<RoutingRuleDraftLine>,
) {
    val validLines: List<RoutingRuleDraftLine> =
        lines.filter { it.kind == RoutingRuleLineKind.Domain || it.kind == RoutingRuleLineKind.Cidr }
    val domainCount: Int = lines.count { it.kind == RoutingRuleLineKind.Domain }
    val cidrCount: Int = lines.count { it.kind == RoutingRuleLineKind.Cidr }
    val errorCount: Int = lines.count { it.kind == RoutingRuleLineKind.Invalid }
    val firstError: RoutingRuleDraftLine? = lines.firstOrNull { it.kind == RoutingRuleLineKind.Invalid }
}

object RoutingRuleMigration {
    fun fromLegacyDomains(domains: List<String>, mode: SplitTunnelSitesMode): List<RoutingRule> {
        val target = when (mode) {
            SplitTunnelSitesMode.ProxyOnly -> RoutingRuleTarget.Proxy
            SplitTunnelSitesMode.DirectBypass -> RoutingRuleTarget.Direct
        }
        return domains.map { domain ->
            RoutingRule(
                value = domain,
                matcher = RoutingRuleMatcher.DomainSuffix,
                target = target,
            )
        }
    }
}
