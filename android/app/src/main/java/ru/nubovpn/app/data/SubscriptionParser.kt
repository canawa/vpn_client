package ru.nubovpn.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import ru.nubovpn.app.util.AppLog

object SubscriptionParser {
    internal val NO_FLOW_TRANSPORTS = setOf("xhttp", "splithttp", "grpc", "ws", "httpupgrade")

    fun parse(body: String): List<ProxyNode> {
        AppLog.i("SubscriptionParser.parse bodyLen=${body.length} prefix=${body.take(32)}")
        val normalized = body.trim().removePrefix("\uFEFF").trim()
        if (normalized.isBlank()) error("Пустой ответ подписки")

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
                    line.startsWith("vless://", ignoreCase = true) ->
                        listOfNotNull(VlessParser.parse(line))
                    line.startsWith("hy2://", ignoreCase = true) ||
                        line.startsWith("hysteria2://", ignoreCase = true) ->
                        listOfNotNull(Hysteria2Parser.parseUri(line))
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

        error("Неизвестный формат подписки")
    }

    private fun parsePlainText(body: String): List<ProxyNode>? {
        val nodes = body.lineSequence()
            .map { it.trim() }
            .mapNotNull { line ->
                when {
                    line.startsWith("vless://", ignoreCase = true) -> VlessParser.parse(line)
                    line.startsWith("hy2://", ignoreCase = true) ||
                        line.startsWith("hysteria2://", ignoreCase = true) ->
                        Hysteria2Parser.parseUri(line)
                    else -> null
                }
            }
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
            .ifBlank { "Сервер ${index + 1}" }
    }

    private fun extractNodesFromProfile(profile: JSONObject, profileName: String): List<ProxyNode> {
        val outbounds = profile.optJSONArray("outbounds") ?: return emptyList()

        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            if (outbound.optString("tag") != "proxy") continue
            parseOutbound(outbound, profileName)?.let { return listOf(it) }
        }

        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            parseOutbound(outbound, profileName)?.let { return listOf(it) }
        }

        return emptyList()
    }

    private fun parseOutbound(outbound: JSONObject, profileName: String): ProxyNode? =
        parseSingBoxOutbound(outbound, profileName)
            ?: parseXrayHysteriaOutbound(outbound, profileName)
            ?: parseXrayVlessOutbound(outbound, profileName)

    private fun parseXrayVlessOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        if (outbound.optString("protocol") != "vless") return null

        val vnext = outbound.optJSONObject("settings")
            ?.optJSONArray("vnext")
            ?.optJSONObject(0) ?: return null
        val user = vnext.optJSONArray("users")?.optJSONObject(0) ?: return null
        val stream = outbound.optJSONObject("streamSettings") ?: JSONObject()
        val reality = stream.optJSONObject("realitySettings") ?: JSONObject()
        val xhttpSettings = stream.optJSONObject("xhttpSettings")
            ?: stream.optJSONObject("splithttpSettings")
        val transportInfo = TransportParseHelper.fromXrayStream(stream)
        val xhttp = if (transportInfo.type in setOf("xhttp", "splithttp")) {
            XhttpParseHelper.parseFromSettings(xhttpSettings)
        } else {
            null
        }
        val tag = outbound.optString("tag")
        val host = vnext.optString("address")
        val port = vnext.optInt("port")
        val flow = user.optString("flow").takeIf {
            it.isNotBlank() && transportInfo.type !in NO_FLOW_TRANSPORTS
        }

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vless|${user.optString("id")}"),
            name = profileName,
            protocol = "vless",
            uuid = user.optString("id"),
            host = host,
            port = port,
            encryption = user.optString("encryption", "none"),
            flow = flow,
            security = stream.optString("security", "none"),
            sni = reality.optString("serverName").takeIf { it.isNotBlank() }
                ?: stream.optJSONObject("tlsSettings")?.optString("serverName"),
            fingerprint = reality.optString("fingerprint").takeIf { it.isNotBlank() },
            publicKey = reality.optString("publicKey").takeIf { it.isNotBlank() },
            shortId = reality.optString("shortId").takeIf { it.isNotBlank() },
            spiderX = reality.optString("spiderX").takeIf { it.isNotBlank() },
            transport = transportInfo.type,
            grpcServiceName = transportInfo.grpcServiceName,
            grpcAuthority = transportInfo.grpcAuthority,
            grpcMultiMode = transportInfo.grpcMultiMode,
            wsPath = transportInfo.wsPath,
            wsHost = transportInfo.wsHost,
            xhttpHost = xhttp?.host,
            xhttpPath = xhttp?.path,
            xhttpMode = xhttp?.mode,
            xhttpExtra = xhttp?.extra,
            rawOutboundJson = outbound.toString(),
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
            rawOutboundJson = outbound.toString(),
        )
    }

    private fun parseSingBoxOutbound(outbound: JSONObject, profileName: String): ProxyNode? {
        return when (outbound.optString("type")) {
            "hysteria2" -> parseSingBoxHysteria2(outbound, profileName)
            "vless" -> parseSingBoxVless(outbound, profileName)
            else -> null
        }
    }

    private fun parseSingBoxVless(outbound: JSONObject, profileName: String): ProxyNode? {
        val tls = outbound.optJSONObject("tls") ?: JSONObject()
        val reality = tls.optJSONObject("reality") ?: JSONObject()
        val transportJson = outbound.optJSONObject("transport")
        val transportInfo = TransportParseHelper.fromSingBoxTransport(transportJson)
        val xhttp = if (transportInfo.type in setOf("xhttp", "splithttp")) {
            XhttpParseHelper.parseFromTransport(transportJson)
        } else {
            null
        }
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
            .takeIf { it.isNotBlank() && transportInfo.type !in NO_FLOW_TRANSPORTS }

        if (host.isBlank() || uuid.isBlank()) return null

        return ProxyNode(
            id = stableId("$profileName|$tag|$host|$port|vless|$uuid"),
            name = profileName,
            protocol = "vless",
            uuid = uuid,
            host = host,
            port = port,
            encryption = outbound.optString("encryption", "none"),
            flow = flow,
            security = when {
                reality.optBoolean("enabled") -> "reality"
                tls.optBoolean("enabled") -> "tls"
                else -> outbound.optString("security", "none")
            },
            sni = tls.optString("server_name").takeIf { it.isNotBlank() },
            fingerprint = tls.optJSONObject("utls")?.optString("fingerprint"),
            publicKey = reality.optString("public_key").takeIf { it.isNotBlank() },
            shortId = reality.optString("short_id").takeIf { it.isNotBlank() },
            transport = transportInfo.type,
            grpcServiceName = transportInfo.grpcServiceName,
            grpcAuthority = transportInfo.grpcAuthority,
            grpcMultiMode = transportInfo.grpcMultiMode,
            wsPath = transportInfo.wsPath,
            wsHost = transportInfo.wsHost,
            xhttpHost = xhttp?.host,
            xhttpPath = xhttp?.path,
            xhttpMode = xhttp?.mode,
            xhttpExtra = xhttp?.extra,
            rawOutboundJson = outbound.toString(),
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
