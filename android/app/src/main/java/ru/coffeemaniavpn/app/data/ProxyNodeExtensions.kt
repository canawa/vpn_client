package ru.coffeemaniavpn.app.data

/** Для share-ссылок без JSON-профиля — заранее собираем Xray-outbound. */
internal fun ProxyNode.withBuiltOutbound(): ProxyNode {
    if (!rawOutboundJson.isNullOrBlank()) return this
    return runCatching {
        copy(rawOutboundJson = XrayConfigBuilder.buildProxyOutbound(this).toString())
    }.getOrDefault(this)
}

internal fun List<ProxyNode>.withBuiltOutbounds(): List<ProxyNode> =
    map { it.withBuiltOutbound() }
