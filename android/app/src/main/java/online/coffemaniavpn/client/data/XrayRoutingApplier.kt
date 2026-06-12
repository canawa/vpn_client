package online.coffemaniavpn.client.data

import org.json.JSONArray
import org.json.JSONObject
import online.coffemaniavpn.client.util.AppLog

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
        applySiteRules(config, settings)
        applySiteDns(config, settings)
    }

    private fun applySiteRules(config: JSONObject, settings: ConnectionSettingsState) {
        if (!settings.sitesEnabled || settings.siteDomains.isEmpty()) return

        val routing = config.optJSONObject("routing") ?: return
        val rules = routing.optJSONArray("rules") ?: JSONArray().also { routing.put("rules", it) }

        val outbound = when (settings.sitesMode) {
            SplitTunnelSitesMode.ProxyOnly -> "proxy"
            SplitTunnelSitesMode.DirectBypass -> "direct"
        }
        val siteRules = JSONArray()
        settings.siteDomains.forEach { raw ->
            val domain = normalizeDomain(raw)
            if (domain.isBlank()) return@forEach
            siteRules.put(
                XrayConfigBuilder.fieldRule(
                    domain = JSONArray().apply {
                        put("domain:$domain")
                        put("domain:.$domain")
                    },
                    outboundTag = outbound,
                ),
            )
        }

        val merged = JSONArray()
        for (i in 0 until rules.length()) merged.put(rules.get(i))
        for (i in 0 until siteRules.length()) merged.put(siteRules.get(i))
        routing.put("rules", merged)

        val finalOutbound = when (settings.sitesMode) {
            SplitTunnelSitesMode.ProxyOnly -> "direct"
            SplitTunnelSitesMode.DirectBypass -> "proxy"
        }
        putFinalRule(routing, finalOutbound)
    }

    private fun applySiteDns(config: JSONObject, settings: ConnectionSettingsState) {
        if (!settings.sitesEnabled || settings.siteDomains.isEmpty()) return
        val dns = config.optJSONObject("dns") ?: return
        val servers = dns.optJSONArray("servers") ?: JSONArray().also { dns.put("servers", it) }

        val merged = JSONArray()
        settings.siteDomains.forEach { raw ->
            val domain = normalizeDomain(raw)
            if (domain.isBlank()) return@forEach
            merged.put(JSONObject().apply {
                put("address", "8.8.8.8")
                put("domains", JSONArray().apply {
                    put("domain:$domain")
                    put("domain:.$domain")
                })
                put("skipFallback", true)
            })
        }
        for (i in 0 until servers.length()) merged.put(servers.get(i))
        dns.put("servers", merged)
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
