package online.coffemaniavpn.client.vpn

object VpnAction {
    const val SERVICE_CLOSE = "online.coffemaniavpn.client.action.SERVICE_CLOSE"
}

enum class VpnStatus {
    Stopped,
    Starting,
    Started,
    Stopping,
}
