package ru.coffeemaniavpn.app.vpn

object VpnAction {
    const val SERVICE_CLOSE = "ru.coffeemaniavpn.app.action.SERVICE_CLOSE"
    const val EXTRA_USER_INITIATED = "ru.coffeemaniavpn.app.extra.USER_INITIATED"
}

enum class VpnStatus {
    Stopped,
    Starting,
    Started,
    Stopping,
}
