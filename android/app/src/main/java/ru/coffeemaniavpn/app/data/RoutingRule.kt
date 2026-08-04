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

    private fun stripPrefixIgnoreCase(value: String, prefix: String): String =
        if (value.startsWith(prefix, ignoreCase = true)) {
            value.substring(prefix.length)
        } else {
            value
        }
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
