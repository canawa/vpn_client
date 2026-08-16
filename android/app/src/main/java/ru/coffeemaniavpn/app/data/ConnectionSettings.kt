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
    val sitesEnabled: Boolean = true,
    val sitesMode: SplitTunnelSitesMode = SplitTunnelSitesMode.ProxyOnly,
    val siteDomains: List<String> = emptyList(),
    val customRules: List<RoutingRule> = emptyList(),
    val appsEnabled: Boolean = true,
    val appsMode: SplitTunnelAppsMode = SplitTunnelAppsMode.ExcludeSelected,
    val appPackages: Set<String> = emptySet(),
    val killSwitchEnabled: Boolean = false,
    /** Готовый список: российские сервисы → напрямую */
    val presetRuDirect: Boolean = true,
    /** Готовый список: реклама и трекеры → блок */
    val presetAdsBlock: Boolean = false,
) {
    /**
     * Число активных элементов умного роутинга для UI:
     * domain-правила, пресеты (в CUSTOM) и выбранные приложения.
     */
    fun activeSmartRoutingCount(mode: TrafficRoutingMode): Int {
        var count = 0
        if (mode == TrafficRoutingMode.CUSTOM) {
            count += customRules.count { it.isEnabled }
            if (presetRuDirect) count += 1
            if (presetAdsBlock) count += 1
        }
        if (appsEnabled) {
            count += appPackages.size.coerceAtLeast(1)
        }
        return count
    }
}
