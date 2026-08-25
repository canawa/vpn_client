package work.bavshield.vpn.vpn

object VpnAction {
    const val SERVICE_CLOSE = "work.bavshield.vpn.action.SERVICE_CLOSE"
}

enum class VpnStatus {
    Stopped,
    Starting,
    Started,
    Stopping,
}
