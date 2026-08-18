package ru.coffeemaniavpn.app.widget

import ru.coffeemaniavpn.app.vpn.VpnStatus

/** Кадр анимации кнопки виджета (зеркало XenoConnectButton). */
data class WidgetConnectAnimState(
    val spinAngle: Float = 0f,
    val labelMode: LabelMode = LabelMode.Off,
) {
    enum class LabelMode { Off, Busy, On }

    companion object {
        fun snapped(status: VpnStatus): WidgetConnectAnimState = when (status) {
            VpnStatus.Started -> WidgetConnectAnimState(labelMode = LabelMode.On)
            VpnStatus.Starting, VpnStatus.Stopping -> WidgetConnectAnimState(labelMode = LabelMode.Busy)
            VpnStatus.Stopped -> WidgetConnectAnimState(labelMode = LabelMode.Off)
        }
    }
}
