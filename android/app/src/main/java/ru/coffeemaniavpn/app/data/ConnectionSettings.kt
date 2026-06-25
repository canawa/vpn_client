package ru.coffeemaniavpn.app.data

enum class SplitTunnelSitesMode {
    /** Только перечисленные сайты идут через VPN */
    ProxyOnly,

    /** Перечисленные сайты идут в обход VPN */
    DirectBypass,
}

enum class SplitTunnelAppsMode {
    /** Через VPN только выбранные приложения */
    IncludeOnly,

    /** Через VPN все приложения, кроме выбранных */
    ExcludeSelected,
}

data class ConnectionSettingsState(
    val sitesEnabled: Boolean = false,
    val sitesMode: SplitTunnelSitesMode = SplitTunnelSitesMode.ProxyOnly,
    val siteDomains: List<String> = emptyList(),
    val appsEnabled: Boolean = false,
    val appsMode: SplitTunnelAppsMode = SplitTunnelAppsMode.IncludeOnly,
    val appPackages: Set<String> = emptySet(),
    val killSwitchEnabled: Boolean = false,
)
