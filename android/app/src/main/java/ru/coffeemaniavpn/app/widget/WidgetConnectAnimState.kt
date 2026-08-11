package ru.coffeemaniavpn.app.widget

import ru.coffeemaniavpn.app.vpn.VpnStatus

/** Кадр анимации кнопки виджета (зеркало ClevConnectButton). */
data class WidgetConnectAnimState(
    val plateOn: Float = 0f,
    val ringFill: Float = 0f,
    val spinAngle: Float = 0f,
    val cometAngle: Float = -90f,
    val showComet: Boolean = false,
    val showSpinner: Boolean = false,
    val haloPulse: Float = 1f,
    val burstAlpha: Float = 0f,
    val labelMode: LabelMode = LabelMode.Off,
) {
    enum class LabelMode { Off, Busy, On }

    companion object {
        fun snapped(status: VpnStatus): WidgetConnectAnimState = when (status) {
            VpnStatus.Started -> WidgetConnectAnimState(
                plateOn = 1f,
                ringFill = 1f,
                labelMode = LabelMode.On,
            )
            VpnStatus.Starting, VpnStatus.Stopping -> WidgetConnectAnimState(
                showSpinner = true,
                labelMode = LabelMode.Busy,
            )
            VpnStatus.Stopped -> WidgetConnectAnimState(
                labelMode = LabelMode.Off,
            )
        }
    }
}
