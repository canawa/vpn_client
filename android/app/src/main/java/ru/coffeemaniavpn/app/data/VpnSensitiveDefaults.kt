package ru.coffeemaniavpn.app.data

import android.content.pm.PackageManager

/**
 * Приложения и домены, которые часто ломаются или блокируют работу через VPN
 * (банки, госуслуги и т.п.) — по умолчанию в обход туннеля.
 */
object VpnSensitiveDefaults {
    const val VERSION = 1

    val appPackages: Set<String> = setOf(
        // Банки
        "ru.sberbankmobile",
        "com.idamob.tinkoff.android",
        "ru.tinkoff.investing",
        "ru.alfabank.mobile.android",
        "ru.vtb24.mobilebanking.android",
        "ru.vtb.mobilebanking",
        "com.vtb.mobilebanking",
        "ru.raiffeisennews",
        "com.bssys.raiffeisenonline",
        "ru.gazprombank.android.mobilebank.app",
        "ru.rosbank.android",
        "ru.rshb.dbo",
        "ru.sovcomcard.halva",
        "ru.sovcomcard.android",
        "ru.open.mobile",
        "ru.yoo.money",
        "com.yandex.bank",
        "ru.mkb.mobile",
        "com.unicredit",
        "ru.otpbank.mobile",
        "ru.homecredit.ibank",
        "ru.letobank.Prometheus",
        "com.bss.vbrrpay",
        "logo.com.mbanking",
        // Госуслуги / госсектор
        "ru.gosuslugi.app.gosuslugi",
        "ru.rostel",
        "ru.mos.udobno",
        "ru.mos.portal",
        "com.bpc.svbp.android",
        "ru.nalog",
        // Платежи / маркетплейсы с жёстким анти-VPN
        "ru.wildberries.client",
        "com.wildberries.ru",
        "ru.ozon.app.android",
        "ru.dns.shop.android",
        "com.avito.android",
        // Мессенджеры банковских уведомлений / экосистемы
        "ru.sberbank.sberbankid",
        "ru.max.android",
    )

    val directDomains: List<String> = listOf(
        "sberbank.ru",
        "online.sberbank.ru",
        "sber.ru",
        "tbank.ru",
        "tinkoff.ru",
        "alfabank.ru",
        "alfabank.com",
        "vtb.ru",
        "vtb24.ru",
        "raiffeisen.ru",
        "gazprombank.ru",
        "rshb.ru",
        "open.ru",
        "sovcombank.ru",
        "yoomoney.ru",
        "bank.yandex.ru",
        "gosuslugi.ru",
        "mos.ru",
        "nalog.ru",
        "cbr.ru",
        "gu-st.ru",
        "wildberries.ru",
        "ozon.ru",
        "avito.ru",
        "dns-shop.ru",
    )

    fun apply(
        current: ConnectionSettingsState,
        packageManager: PackageManager,
    ): ConnectionSettingsState {
        val installedSensitive = appPackages.filter { pkg ->
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(pkg, 0)
                }
                true
            }.getOrDefault(false)
        }.toSet()

        val existingDomainValues = current.customRules
            .map { it.value.lowercase() }
            .toSet()
        val missingDomainRules = directDomains
            .filter { it.lowercase() !in existingDomainValues }
            .map { domain ->
                RoutingRule(
                    value = domain,
                    matcher = RoutingRuleMatcher.DomainSuffix,
                    target = RoutingRuleTarget.Direct,
                    isEnabled = true,
                )
            }

        return current.copy(
            appsEnabled = true,
            appsMode = SplitTunnelAppsMode.ExcludeSelected,
            appPackages = current.appPackages + installedSensitive,
            sitesEnabled = true,
            customRules = current.customRules + missingDomainRules,
        )
    }
}
