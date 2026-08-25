package work.bavshield.vpn.data

object ShareLinkParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> VlessParser.parse(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> VmessParser.parse(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> TrojanParser.parse(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> ShadowsocksParser.parse(trimmed)
            trimmed.startsWith("socks://", ignoreCase = true) ||
                trimmed.startsWith("socks5://", ignoreCase = true) -> SocksHttpParser.parseSocks(trimmed)
            trimmed.startsWith("http://", ignoreCase = true) -> SocksHttpParser.parseHttp(trimmed)
            trimmed.startsWith("hy2://", ignoreCase = true) ||
                trimmed.startsWith("hysteria2://", ignoreCase = true) -> Hysteria2Parser.parseUri(trimmed)
            else -> null
        }
    }

    fun looksLikeShareLink(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("vless://", ignoreCase = true) ||
            trimmed.startsWith("vmess://", ignoreCase = true) ||
            trimmed.startsWith("trojan://", ignoreCase = true) ||
            trimmed.startsWith("ss://", ignoreCase = true) ||
            trimmed.startsWith("socks://", ignoreCase = true) ||
            trimmed.startsWith("socks5://", ignoreCase = true) ||
            trimmed.startsWith("hy2://", ignoreCase = true) ||
            trimmed.startsWith("hysteria2://", ignoreCase = true) ||
            (trimmed.startsWith("http://", ignoreCase = true) && trimmed.contains('@'))
    }
}
