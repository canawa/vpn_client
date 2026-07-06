package ru.nubovpn.app.vpn

object VpnAction {
    const val SERVICE_CLOSE = "ru.nubovpn.app.action.SERVICE_CLOSE"
}

enum class VpnStatus {
    Stopped,
    Starting,
    Started,
    Stopping,
}
