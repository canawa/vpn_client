package online.coffemaniavpn.client.data

import org.json.JSONArray
import org.json.JSONObject
import online.coffemaniavpn.client.util.AppLog

object RoutingProfileStore {
    @Volatile
    var activeProfileJson: String? = null
        private set

    fun updateActive(json: String?) {
        activeProfileJson = json?.takeIf { it.isNotBlank() }
        AppLog.i("RoutingProfileStore update active=${activeProfileJson?.length ?: 0}")
    }

    fun applyToConfig(config: JSONObject) {
        val raw = activeProfileJson ?: return
        val profile = runCatching { JSONObject(raw) }.getOrElse {
            AppLog.e("RoutingProfileStore invalid json", it)
            return
        }

        val route = config.optJSONObject("route") ?: JSONObject().also { config.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

        val blockRules = JSONArray()
        val proxyRules = JSONArray()
        val directRules = JSONArray()

        profile.optJSONArray("BlockSites")?.let { addGeositeRules(it, "block", blockRules) }
        profile.optJSONArray("BlockIp")?.let { addGeoipRules(it, "block", blockRules) }
        profile.optJSONArray("ProxySites")?.let { addGeositeRules(it, "proxy", proxyRules) }
        profile.optJSONArray("ProxyIp")?.let { addGeoipRules(it, "proxy", proxyRules) }
        profile.optJSONArray("DirectSites")?.let { addGeositeRules(it, "direct", directRules) }
        profile.optJSONArray("DirectIp")?.let { addGeoipRules(it, "direct", directRules) }

        val merged = JSONArray()
        appendRules(merged, blockRules)
        appendRules(merged, proxyRules)
        appendRules(merged, directRules)

        for (i in 0 until rules.length()) {
            merged.put(rules.get(i))
        }
        route.put("rules", merged)

        val globalProxy = profile.optBoolean("GlobalProxy", true)
        route.put("final", if (globalProxy) "proxy" else "direct")

        applyDns(config, profile)
        AppLog.i("RoutingProfileStore applied name=${profile.optString("Name")} rules=${merged.length()}")
    }

    private fun addGeositeRules(source: JSONArray, outbound: String, target: JSONArray) {
        for (i in 0 until source.length()) {
            val entry = source.optString(i).orEmpty()
            if (entry.isBlank()) continue
            val tag = entry.substringAfter(':', missingDelimiterValue = entry)
            target.put(
                JSONObject().apply {
                    put("geosite", tag)
                    put("outbound", outbound)
                },
            )
        }
    }

    private fun addGeoipRules(source: JSONArray, outbound: String, target: JSONArray) {
        for (i in 0 until source.length()) {
            val entry = source.optString(i).orEmpty()
            if (entry.isBlank()) continue
            val tag = entry.substringAfter(':', missingDelimiterValue = entry)
            target.put(
                JSONObject().apply {
                    put("geoip", tag)
                    put("outbound", outbound)
                },
            )
        }
    }

    private fun appendRules(target: JSONArray, source: JSONArray) {
        for (i in 0 until source.length()) {
            target.put(source.get(i))
        }
    }

    private fun applyDns(config: JSONObject, profile: JSONObject) {
        val remoteDns = profile.optString("RemoteDns").trim()
        val domesticDns = profile.optString("DomesticDns").trim()
        if (remoteDns.isBlank() && domesticDns.isBlank()) return

        val dns = config.optJSONObject("dns") ?: return
        val servers = JSONArray()

        if (domesticDns.isNotBlank()) {
            servers.put(
                JSONObject().apply {
                    put("tag", "dns-direct")
                    put("type", "udp")
                    put("server", domesticDns)
                },
            )
        }

        servers.put(
            JSONObject().apply {
                put("tag", "dns-local")
                put("type", "local")
            },
        )

        if (remoteDns.isNotBlank()) {
            servers.put(
                JSONObject().apply {
                    put("tag", "dns-remote")
                    put("type", "udp")
                    put("server", remoteDns)
                    put("detour", "proxy")
                },
            )
        }

        dns.put("servers", servers)
        dns.put("final", if (remoteDns.isNotBlank()) "dns-remote" else "dns-local")
    }
}
