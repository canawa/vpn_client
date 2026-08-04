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
