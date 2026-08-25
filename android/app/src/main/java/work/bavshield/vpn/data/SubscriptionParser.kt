package work.bavshield.vpn.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import work.bavshield.vpn.data.LocaleHelper
import work.bavshield.vpn.util.AppLog

object SubscriptionParser {
    private fun loc() = LocaleHelper.strings(work.bavshield.vpn.App.instance)

    fun parse(body: String): List<ProxyNode> {
        AppLog.i("SubscriptionParser.parse bodyLen=${body.length} prefix=${body.take(32)}")
        val normalized = body.trim().removePrefix("\uFEFF").trim()
        if (normalized.isBlank()) error(loc().getString(work.bavshield.vpn.R.string.error_subscription_blank))

        parsePlainText(normalized)?.let { return it }
        parseJson(normalized)?.let { return it }

        decodeBase64(normalized)?.let { decoded ->
            parsePlainText(decoded)?.let { return it }
            parseJson(decoded)?.let { return it }
        }

        val perLine = normalized.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .flatMap { line ->
                when {
                    ShareLinkParser.looksLikeShareLink(line) ->
                        listOfNotNull(ShareLinkParser.parse(line))
                    line.startsWith("[") || line.startsWith("{") ->
                        parseJson(line).orEmpty()
                    else ->
                        decodeBase64(line)?.let { decoded ->
                            parsePlainText(decoded).orEmpty() + parseJson(decoded).orEmpty()
                        }.orEmpty()
                }
            }
            .distinctBy { it.id }

        if (perLine.isNotEmpty()) return perLine.also { AppLog.i("SubscriptionParser parsed ${it.size} nodes from lines") }

        error(loc().getString(work.bavshield.vpn.R.string.error_unknown_subscription_format))
    }

    private fun parsePlainText(body: String): List<ProxyNode>? {
        val nodes = body.lineSequence()
            .map { it.trim() }
            .mapNotNull { line -> ShareLinkParser.parse(line) }
            .distinctBy { it.id }
            .toList()

        return nodes.takeIf { it.isNotEmpty() }
    }

    private fun parseJson(body: String): List<ProxyNode>? {
        if (!body.startsWith("[") && !body.startsWith("{")) return null
        val nodes = mutableListOf<ProxyNode>()
        when {
            body.startsWith("[") -> {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val profile = array.optJSONObject(i) ?: continue
                    val name = resolveProfileName(profile, i)
                    val parsed = extractNodesFromProfile(profile, name)
                    if (parsed.isEmpty()) {
                        AppLog.w("SubscriptionParser skipped profile index=$i name=$name")
                    } else {
                        nodes += parsed.first()
                    }
                }
                if (array.length() != nodes.size) {
                    AppLog.i(
                        "SubscriptionParser profiles=${array.length()} parsed=${nodes.size}",
                    )
                }
            }
            else -> {
                val profile = JSONObject(body)
                val name = resolveProfileName(profile, 0)
                extractNodesFromProfile(profile, name).firstOrNull()?.let { nodes += it }
            }
        }
        return nodes.distinctBy { it.id }
            .takeIf { it.isNotEmpty() }
            ?.also { AppLog.i("SubscriptionParser JSON parsed ${it.size} nodes") }
    }

    private fun resolveProfileName(profile: JSONObject, index: Int): String {
        return profile.optString("remarks")
            .ifBlank { profile.optString("remark") }
            .ifBlank { profile.optString("ps") }
            .ifBlank { profile.optJSONObject("meta")?.optString("description").orEmpty() }
            .ifBlank { loc().getString(work.bavshield.vpn.R.string.server_fallback, index + 1) }
    }

    private fun extractNodesFromProfile(profile: JSONObject, profileName: String): List<ProxyNode> {
        parseSingBoxOutbound(profile, profileName)?.let { return listOf(it) }
        parseXrayOutbound(profile, profileName)?.let { return listOf(it) }

        val outbounds = profile.optJSONArray("outbounds") ?: return emptyList()

        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            parseSingBoxOutbound(outbound, profileName)?.let { return listOf(it) }
            parseXrayOutbound(outbound, profileName)?.let { return listOf(it) }
        }

        return emptyList()
    }

    private fun parseXrayOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        return when (outbound.optString("protocol").lowercase()) {
            "vless" -> parseXrayVlessOutbound(outbound, profileName)
            "vmess" -> parseXrayVmessOutbound(outbound, profileName)
            "trojan" -> parseXrayTrojanOutbound(outbound, profileName)
            "shadowsocks" -> parseXrayShadowsocksOutbound(outbound, profileName)
            "socks" -> parseXraySocksHttpOutbound(outbound, profileName, "socks")
            "http" -> parseXraySocksHttpOutbound(outbound, profileName, "http")
            "hysteria", "hysteria2" -> parseXrayHysteriaOutbound(outbound, profileName)
            else -> null
        }
    }

    private fun parseXrayVlessOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        if (outbound.optString("protocol").lowercase() != "vless") return null

        val vnext = outbound.optJSONObject("settings")
            ?.optJSONArray("vnext")
            ?.optJSONObject(0) ?: return null
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return null
        val stream = StreamSettingsCodec.parse(outbound.optJSONObject("streamSettings"))
        val tag = outbound.optString("tag")
        val host = vnext.optString("address")
        val port = vnext.optInt("port")
        val flow = user.optString("flow").takeIf { it.isNotBlank() && stream.network != "xhttp" }

        if (host.isBlank() || user.optString("id").isBlank()) return null

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vless|${user.optString("id")}"),
            name = profileName,
            protocol = "vless",
            uuid = user.optString("id"),
            host = host,
            port = port,
            encryption = user.optString("encryption", "none"),
            flow = flow,
        ).withStream(stream)
    }

    private fun parseXrayVmessOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        val vnext = outbound.optJSONObject("settings")
            ?.optJSONArray("vnext")
            ?.optJSONObject(0) ?: return null
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return null
        val host = vnext.optString("address")
        val port = vnext.optInt("port")
        val uuid = user.optString("id")
        if (host.isBlank() || uuid.isBlank()) return null
        val tag = outbound.optString("tag")
        val stream = StreamSettingsCodec.parse(outbound.optJSONObject("streamSettings"))
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vmess|$uuid"),
            name = profileName,
            protocol = "vmess",
            uuid = uuid,
            host = host,
            port = port,
            encryption = user.optString("security").ifBlank { user.optString("encryption", "auto") },
            alterId = user.optInt("alterId", 0),
        ).withStream(stream)
    }

    private fun parseXrayTrojanOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        val server = outbound.optJSONObject("settings")
            ?.optJSONArray("servers")
            ?.optJSONObject(0) ?: return null
        val host = server.optString("address")
        val port = server.optInt("port")
        val password = server.optString("password")
        if (host.isBlank() || password.isBlank()) return null
        val tag = outbound.optString("tag")
        val stream = StreamSettingsCodec.parse(outbound.optJSONObject("streamSettings"))
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|trojan|$password"),
            name = profileName,
            protocol = "trojan",
            host = host,
            port = port,
            password = password,
            uuid = password,
        ).withStream(stream)
    }

    private fun parseXrayShadowsocksOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        val server = outbound.optJSONObject("settings")
            ?.optJSONArray("servers")
            ?.optJSONObject(0)
            ?: outbound.optJSONObject("settings")
            ?: return null
        val host = server.optString("address").ifBlank { server.optString("server") }
        val port = if (server.has("port")) server.optInt("port") else server.optInt("server_port")
        val password = server.optString("password")
        val method = server.optString("method").ifBlank { server.optString("cipher") }
        if (host.isBlank() || password.isBlank() || method.isBlank()) return null
        val tag = outbound.optString("tag")
        val stream = StreamSettingsCodec.parse(outbound.optJSONObject("streamSettings"))
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|ss|$method|$password"),
            name = profileName,
            protocol = "shadowsocks",
            host = host,
            port = port,
            encryption = method,
            password = password,
            uuid = password,
        ).withStream(stream)
    }

    private fun parseXraySocksHttpOutbound(
        outbound: JSONObject,
        profileName: String,
        protocol: String,
    ): ProxyNode? {
        val server = outbound.optJSONObject("settings")
            ?.optJSONArray("servers")
            ?.optJSONObject(0) ?: return null
        val host = server.optString("address")
        val port = server.optInt("port")
        if (host.isBlank() || port <= 0) return null
        val user = server.optJSONArray("users")?.optJSONObject(0)
        val tag = outbound.optString("tag")
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|$protocol"),
            name = profileName,
            protocol = protocol,
            host = host,
            port = port,
            username = user?.optString("user")?.takeIf { it.isNotBlank() },
            password = user?.optString("pass")?.takeIf { it.isNotBlank() },
            uuid = user?.optString("pass").orEmpty(),
            security = "none",
            transport = "tcp",
        )
    }

    private fun parseXrayHysteriaOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        val protocol = outbound.optString("protocol")
        if (protocol != "hysteria" && protocol != "hysteria2") return null

        val settings = outbound.optJSONObject("settings") ?: return null
        val stream = outbound.optJSONObject("streamSettings") ?: JSONObject()
        val hysteria = stream.optJSONObject("hysteriaSettings")
            ?: stream.optJSONObject("hysteria2Settings")
            ?: JSONObject()

        val version = when {
            settings.has("version") -> settings.optInt("version")
            hysteria.has("version") -> hysteria.optInt("version")
            protocol == "hysteria2" -> 2
            else -> 1
        }
        if (version != 2) return null

        val host = settings.optString("address")
            .ifBlank { settings.optString("server") }
        val port = when {
            settings.has("port") -> settings.optInt("port")
            settings.has("server_port") -> settings.optInt("server_port")
            else -> 443
        }
        val auth = hysteria.optString("auth")
            .ifBlank { settings.optString("auth") }
            .ifBlank { settings.optString("password") }
            .ifBlank { hysteria.optString("password") }

        if (host.isBlank() || auth.isBlank()) return null

        val tls = stream.optJSONObject("tlsSettings") ?: JSONObject()
        val obfs = hysteria.optJSONObject("obfs")
            ?: stream.optJSONObject("obfs")
            ?: hysteria.optJSONObject("salamander")
        val quicParams = stream.optJSONObject("finalmask")
            ?.optJSONObject("quicParams")
            ?: JSONObject()
        val congestion = quicParams.optString("congestion")
        val useBbr = congestion.equals("bbr", ignoreCase = true) ||
            congestion.isBlank() && !settings.has("up_mbps") && !settings.has("down_mbps")

        val tag = outbound.optString("tag")

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|hysteria2|$auth"),
            name = profileName,
            protocol = "hysteria2",
            host = host,
            port = port,
            password = auth,
            uuid = auth,
            sni = tls.optString("serverName").ifBlank { host },
            fingerprint = tls.optString("fingerprint").takeIf { it.isNotBlank() },
            obfsType = obfs?.optString("type")?.ifBlank { "salamander" },
            obfsPassword = obfs?.optString("password")?.ifBlank { obfs.optString("auth") },
            insecureTls = tls.optBoolean("allowInsecure") || tls.optBoolean("insecure"),
            upMbps = if (useBbr) null else readPositiveInt(settings, "up_mbps", "up"),
            downMbps = if (useBbr) null else readPositiveInt(settings, "down_mbps", "down"),
            alpn = readStringList(tls.optJSONArray("alpn")),
        )
    }

    private fun parseSingBoxOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        return when (outbound.optString("type")) {
            "hysteria2" -> parseSingBoxHysteria2(outbound, profileName)
            "vless" -> parseSingBoxVless(outbound, profileName)
            "vmess" -> parseSingBoxVmess(outbound, profileName)
            "trojan" -> parseSingBoxTrojan(outbound, profileName)
            "shadowsocks" -> parseSingBoxShadowsocks(outbound, profileName)
            "socks" -> parseSingBoxSocksHttp(outbound, profileName, "socks")
            "http" -> parseSingBoxSocksHttp(outbound, profileName, "http")
            else -> null
        }
    }

    private fun parseSingBoxVless(outbound: JSONObject, profileName: String): ProxyNode? {
        val host = outbound.optString("server")
            .ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = 443)
        val tag = outbound.optString("tag")
        val uuid = outbound.optString("uuid")
            .ifBlank {
                outbound.optJSONArray("users")
                    ?.optJSONObject(0)
                    ?.optString("uuid")
                    .orEmpty()
            }
        val flow = outbound.optString("flow")
            .ifBlank {
                outbound.optJSONArray("users")
                    ?.optJSONObject(0)
                    ?.optString("flow")
                    .orEmpty()
            }
        val stream = StreamSettingsCodec.parseSingBoxTransport(
            outbound.optJSONObject("transport"),
            outbound.optJSONObject("tls"),
        )
        if (host.isBlank() || uuid.isBlank()) return null

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vless|$uuid"),
            name = profileName,
            protocol = "vless",
            uuid = uuid,
            host = host,
            port = port,
            encryption = outbound.optString("encryption", "none"),
            flow = flow.takeIf { it.isNotBlank() && stream.network != "xhttp" },
        ).withStream(stream)
    }

    private fun parseSingBoxVmess(outbound: JSONObject, profileName: String): ProxyNode? {
        val host = outbound.optString("server").ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = 443)
        val uuid = outbound.optString("uuid")
        if (host.isBlank() || uuid.isBlank()) return null
        val stream = StreamSettingsCodec.parseSingBoxTransport(
            outbound.optJSONObject("transport"),
            outbound.optJSONObject("tls"),
        )
        val tag = outbound.optString("tag")
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vmess|$uuid"),
            name = profileName,
            protocol = "vmess",
            uuid = uuid,
            host = host,
            port = port,
            encryption = outbound.optString("security").ifBlank { "auto" },
            alterId = outbound.optInt("alter_id", 0),
        ).withStream(stream)
    }

    private fun parseSingBoxTrojan(outbound: JSONObject, profileName: String): ProxyNode? {
        val host = outbound.optString("server").ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = 443)
        val password = outbound.optString("password")
        if (host.isBlank() || password.isBlank()) return null
        val stream = StreamSettingsCodec.parseSingBoxTransport(
            outbound.optJSONObject("transport"),
            outbound.optJSONObject("tls"),
        )
        val tag = outbound.optString("tag")
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|trojan|$password"),
            name = profileName,
            protocol = "trojan",
            host = host,
            port = port,
            password = password,
            uuid = password,
        ).withStream(stream)
    }

    private fun parseSingBoxShadowsocks(outbound: JSONObject, profileName: String): ProxyNode? {
        val host = outbound.optString("server").ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = 8388)
        val password = outbound.optString("password")
        val method = outbound.optString("method").ifBlank { outbound.optString("cipher") }
        if (host.isBlank() || password.isBlank() || method.isBlank()) return null
        val tag = outbound.optString("tag")
        val stream = StreamSettingsCodec.parseSingBoxTransport(
            outbound.optJSONObject("transport"),
            outbound.optJSONObject("tls"),
        )
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|ss|$method|$password"),
            name = profileName,
            protocol = "shadowsocks",
            host = host,
            port = port,
            encryption = method,
            password = password,
            uuid = password,
        ).withStream(stream)
    }

    private fun parseSingBoxSocksHttp(
        outbound: JSONObject,
        profileName: String,
        protocol: String,
    ): ProxyNode? {
        val host = outbound.optString("server").ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = if (protocol == "http") 8080 else 1080)
        if (host.isBlank()) return null
        val tag = outbound.optString("tag")
        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|$protocol"),
            name = profileName,
            protocol = protocol,
            host = host,
            port = port,
            username = outbound.optString("username").takeIf { it.isNotBlank() },
            password = outbound.optString("password").takeIf { it.isNotBlank() },
            uuid = outbound.optString("password"),
            security = "none",
            transport = "tcp",
        )
    }

    private fun parseSingBoxHysteria2(outbound: JSONObject, profileName: String): ProxyNode? {
        val host = outbound.optString("server")
            .ifBlank { outbound.optString("address") }
        val port = readPort(outbound, "server_port", default = 443)
        val password = outbound.optString("password")
            .ifBlank { outbound.optString("auth") }
            .ifBlank {
                outbound.optJSONArray("users")
                    ?.optJSONObject(0)
                    ?.optString("password")
                    .orEmpty()
            }
        if (host.isBlank() || password.isBlank()) return null

        val tls = outbound.optJSONObject("tls") ?: JSONObject()
        val obfs = outbound.optJSONObject("obfs")
        val tag = outbound.optString("tag")

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|hysteria2|$password"),
            name = profileName,
            protocol = "hysteria2",
            host = host,
            port = port,
            password = password,
            uuid = password,
            sni = tls.optString("server_name").ifBlank { host },
            fingerprint = tls.optJSONObject("utls")?.optString("fingerprint"),
            obfsType = obfs?.optString("type"),
            obfsPassword = obfs?.optString("password"),
            insecureTls = tls.optBoolean("insecure"),
            upMbps = outbound.optInt("up_mbps", 0).takeIf { it > 0 },
            downMbps = outbound.optInt("down_mbps", 0).takeIf { it > 0 },
            alpn = readStringList(tls.optJSONArray("alpn")),
        )
    }

    private fun readPort(json: JSONObject, key: String, default: Int): Int {
        if (!json.has(key)) return default
        return when (val raw = json.opt(key)) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> json.optInt(key, default)
        }.takeIf { it > 0 } ?: default
    }

    private fun readPositiveInt(json: JSONObject, vararg keys: String): Int? {
        for (key in keys) {
            if (json.has(key)) {
                val value = json.optInt(key)
                if (value > 0) return value
            }
        }
        return null
    }

    private fun readStringList(array: JSONArray?): List<String>? {
        if (array == null || array.length() == 0) return null
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun decodeBase64(input: String): String? {
        val normalized = input.replace(Regex("\\s"), "")
        if (normalized.isEmpty() || !normalized.matches(Regex("^[A-Za-z0-9+/_=-]+$"))) {
            return null
        }

        val flags = intArrayOf(
            Base64.DEFAULT,
            Base64.NO_WRAP,
            Base64.URL_SAFE or Base64.NO_WRAP,
        )

        for (flag in flags) {
            try {
                return String(Base64.decode(normalized, flag), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }

    private fun stableId(value: String): String =
        UUID.nameUUIDFromBytes(value.toByteArray()).toString()
}
