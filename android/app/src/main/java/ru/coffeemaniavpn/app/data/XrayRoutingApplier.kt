package ru.coffeemaniavpn.app.data

import org.json.JSONArray
import org.json.JSONObject
import ru.coffeemaniavpn.app.util.AppLog

object XrayRoutingApplier {
    fun applyRoutingProfile(config: JSONObject) {
        val raw = RoutingProfileStore.activeProfileJson ?: return
        val profile = runCatching { JSONObject(raw) }.getOrElse {
            AppLog.e("XrayRoutingApplier invalid profile json", it)
            return
        }

        val routing = config.optJSONObject("routing") ?: return
        val rules = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }

        val prepend = JSONArray()
        profile.optJSONArray("BlockSites")?.let { addGeositeRules(it, "block", prepend) }
        profile.optJSONArray("BlockIp")?.let { addGeoipRules(it, "block", prepend) }
        profile.optJSONArray("ProxySites")?.let { addGeositeRules(it, "proxy", prepend) }
        profile.optJSONArray("ProxyIp")?.let { addGeoipRules(it, "proxy", prepend) }
        profile.optJSONArray("DirectSites")?.let { addGeositeRules(it, "direct", prepend) }
        profile.optJSONArray("DirectIp")?.let { addGeoipRules(it, "direct", prepend) }

        val merged = JSONArray()
        for (i in 0 until prepend.length()) merged.put(prepend.get(i))
        for (i in 0 until rules.length()) merged.put(rules.get(i))
        routing.put("rules", merged)

        val globalProxy = profile.optBoolean("GlobalProxy", true)
        routing.put("domainStrategy", "IPIfNonMatch")
        putFinalRule(routing, if (globalProxy) "proxy" else "direct")

        applyProfileDns(config, profile)
        AppLog.i("XrayRoutingApplier applied name=${profile.optString("Name")} rules=${merged.length()}")
    }

    fun applyConnectionSettings(config: JSONObject) {
        val settings = ConnectionSettingsStore.state
        applyCustomRules(config, settings.customRules.filter { it.isEnabled })
    }

    private fun applyCustomRules(config: JSONObject, rules: List<RoutingRule>) {
        val routing = config.optJSONObject("routing") ?: return
        val existing = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }

        val custom = JSONArray()
        rules.forEach { rule ->
            val outbound = when (rule.target) {
                RoutingRuleTarget.Proxy -> "proxy"
                RoutingRuleTarget.Direct -> "direct"
            }
            when (rule.matcher) {
                RoutingRuleMatcher.DomainSuffix -> {
                    val domain = normalizeDomain(rule.value)
                    if (domain.isBlank()) return@forEach
                    custom.put(
                        XrayConfigBuilder.fieldRule(
                            domain = JSONArray().apply {
                                put("domain:$domain")
                                put("domain:.$domain")
                            },
                            outboundTag = outbound,
                        ),
                    )
                }
                RoutingRuleMatcher.IpCidr -> {
                    val cidr = rule.value.trim()
                    if (cidr.isBlank()) return@forEach
                    custom.put(
                        XrayConfigBuilder.fieldRule(
                            ip = JSONArray().put(cidr),
                            outboundTag = outbound,
                        ),
                    )
                }
            }
        }

        val merged = JSONArray()
        for (i in 0 until existing.length()) merged.put(existing.get(i))
        for (i in 0 until custom.length()) merged.put(custom.get(i))
        routing.put("rules", merged)

        val finalOutbound = when (TrafficRoutingStore.mode) {
            TrafficRoutingMode.GLOBAL -> "proxy"
            TrafficRoutingMode.CUSTOM -> {
                // Правила «через VPN» = whitelist: остальное direct.
                // Только «мимо VPN» = blacklist: остальное через proxy.
                val hasProxyRule = rules.any { it.target == RoutingRuleTarget.Proxy }
                if (hasProxyRule) "direct" else "proxy"
            }
        }
        putFinalRule(routing, finalOutbound)
        AppLog.i(
            "XrayRoutingApplier customRules=${rules.size} mode=${TrafficRoutingStore.mode} final=$finalOutbound",
        )
    }

    private fun putFinalRule(routing: JSONObject, outboundTag: String) {
        val rules = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }
        val filtered = JSONArray()
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val isCatchAll = rule.optString("type") == "field" &&
                rule.optString("network") == "tcp,udp" &&
                !rule.has("domain") &&
                !rule.has("ip")
            if (!isCatchAll) filtered.put(rule)
        }
        filtered.put(JSONObject().apply {
            put("type", "field")
            put("network", "tcp,udp")
            put("outboundTag", outboundTag)
        })
        routing.put("rules", filtered)
    }

    private fun addGeositeRules(source: JSONArray, outbound: String, target: JSONArray) {
        for (i in 0 until source.length()) {
            val entry = source.optString(i).orEmpty()
            if (entry.isBlank()) continue
            val tag = entry.substringAfter(':', missingDelimiterValue = entry)
            target.put(
                XrayConfigBuilder.fieldRule(
                    domain = JSONArray().put("geosite:$tag"),
                    outboundTag = outbound,
                ),
            )
        }
    }

    private fun addGeoipRules(source: JSONArray, outbound: String, target: JSONArray) {
        for (i in 0 until source.length()) {
            val entry = source.optString(i).orEmpty()
            if (entry.isBlank()) continue
            val tag = entry.substringAfter(':', missingDelimiterValue = entry)
            target.put(
                XrayConfigBuilder.fieldRule(
                    ip = JSONArray().put("geoip:$tag"),
                    outboundTag = outbound,
                ),
            )
        }
    }

    private fun applyProfileDns(config: JSONObject, profile: JSONObject) {
        val remoteDns = profile.optString("RemoteDns").trim()
        val domesticDns = profile.optString("DomesticDns").trim()
        if (remoteDns.isBlank() && domesticDns.isBlank()) return

        val dns = config.optJSONObject("dns") ?: return
        val servers = JSONArray()
        if (domesticDns.isNotBlank()) servers.put(domesticDns)
        if (remoteDns.isNotBlank()) servers.put(remoteDns)
        dns.put("servers", servers)
    }

    private fun normalizeDomain(raw: String): String =
        raw.trim().lowercase().removePrefix("www.").removePrefix(".")
}
