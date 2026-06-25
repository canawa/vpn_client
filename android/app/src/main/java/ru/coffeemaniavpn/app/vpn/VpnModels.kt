package ru.coffeemaniavpn.app.vpn

object VpnAction {
    const val SERVICE_CLOSE = "ru.coffeemaniavpn.app.action.SERVICE_CLOSE"
}

enum class VpnStatus {
    Stopped,
    Starting,
    Started,
    Stopping,
}
