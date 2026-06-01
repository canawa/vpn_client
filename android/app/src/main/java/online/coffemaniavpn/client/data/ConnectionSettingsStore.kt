package online.coffemaniavpn.client.data

import org.json.JSONArray
import org.json.JSONObject
import online.coffemaniavpn.client.util.AppLog

object ConnectionSettingsStore {
    @Volatile
    var state: ConnectionSettingsState = ConnectionSettingsState()
        private set

    fun update(newState: ConnectionSettingsState) {
        state = newState
        AppLog.i(
            "ConnectionSettingsStore sites=${newState.sitesEnabled}/${newState.siteDomains.size} " +
                "mode=${newState.sitesMode} " +
                "apps=${newState.appsEnabled}/${newState.appPackages.size} " +
                "killSwitch=${newState.killSwitchEnabled}",
        )
    }

    fun applyToConfig(config: JSONObject) {
        val settings = state
        applySiteRules(config, settings)
        applySiteDns(config, settings)
        applyAppPackages(config, settings)
    }

    private fun applySiteRules(config: JSONObject, settings: ConnectionSettingsState) {
        if (!settings.sitesEnabled || settings.siteDomains.isEmpty()) return

        val route = config.optJSONObject("route") ?: return
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
        val siteRules = buildSiteRouteRules(settings)
        if (siteRules.length() == 0) return

        route.put("rules", mergeRulesAfterSniff(rules, siteRules))

        val finalOutbound = when (settings.sitesMode) {
            SplitTunnelSitesMode.ProxyOnly -> "direct"
            SplitTunnelSitesMode.DirectBypass -> "proxy"
        }
        route.put("final", finalOutbound)
        AppLog.i(
            "ConnectionSettingsStore route mode=${settings.sitesMode} final=$finalOutbound " +
                "domains=${settings.siteDomains.size}",
        )
    }

    private fun buildSiteRouteRules(settings: ConnectionSettingsState): JSONArray {
        val outbound = when (settings.sitesMode) {
            SplitTunnelSitesMode.ProxyOnly -> "proxy"
            SplitTunnelSitesMode.DirectBypass -> "direct"
        }
        val siteRules = JSONArray()
        settings.siteDomains.forEach { raw ->
            val domain = normalizeDomain(raw)
            if (domain.isBlank()) return@forEach
            siteRules.put(
                JSONObject().apply {
                    put("domain", JSONArray().apply { put(domain) })
                    put("domain_suffix", ".$domain")
                    put("outbound", outbound)
                },
            )
        }
        return siteRules
    }

    private fun mergeRulesAfterSniff(existing: JSONArray, siteRules: JSONArray): JSONArray {
        val merged = JSONArray()
        var inserted = false
        for (i in 0 until existing.length()) {
            val rule = existing.optJSONObject(i)
            merged.put(existing.get(i))
            if (!inserted && rule != null && isSniffOrDnsHijackRule(rule)) {
                appendRules(merged, siteRules)
                inserted = true
            }
        }
        if (!inserted) {
            appendRules(merged, siteRules)
        }
        return merged
    }

    private fun isSniffOrDnsHijackRule(rule: JSONObject): Boolean {
        val action = rule.optString("action")
        return action == "sniff" || action == "hijack-dns"
    }

    private fun applySiteDns(config: JSONObject, settings: ConnectionSettingsState) {
        if (!settings.sitesEnabled || settings.siteDomains.isEmpty()) return

        val dns = config.optJSONObject("dns") ?: return
        val existing = dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }

        val dnsServer = when (settings.sitesMode) {
            SplitTunnelSitesMode.ProxyOnly -> "dns-remote"
            SplitTunnelSitesMode.DirectBypass -> "dns-local"
        }

        val merged = JSONArray()
        settings.siteDomains.forEach { raw ->
            val domain = normalizeDomain(raw)
            if (domain.isBlank()) return@forEach
            merged.put(
                JSONObject().apply {
                    put("domain", JSONArray().apply { put(domain) })
                    put("domain_suffix", ".$domain")
                    put("server", dnsServer)
                },
            )
        }
        for (i in 0 until existing.length()) {
            merged.put(existing.get(i))
        }
        dns.put("rules", merged)
    }

    private fun applyAppPackages(config: JSONObject, settings: ConnectionSettingsState) {
        if (!settings.appsEnabled || settings.appPackages.isEmpty()) return

        val inbounds = config.optJSONArray("inbounds") ?: return
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") != "tun") continue

            when (settings.appsMode) {
                SplitTunnelAppsMode.IncludeOnly -> {
                    inbound.put("include_package", JSONArray(settings.appPackages.toList()))
                    inbound.remove("exclude_package")
                }
                SplitTunnelAppsMode.ExcludeSelected -> {
                    inbound.put("exclude_package", JSONArray(settings.appPackages.toList()))
                    inbound.remove("include_package")
                }
            }
            break
        }
    }

    private fun normalizeDomain(raw: String): String =
        raw.trim().lowercase().removePrefix("www.").removePrefix(".")

    private fun appendRules(target: JSONArray, rules: JSONArray) {
        for (i in 0 until rules.length()) {
            target.put(rules.get(i))
        }
    }
}
