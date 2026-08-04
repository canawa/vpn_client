package ru.coffeemaniavpn.app.data

object TrafficRoutingStore {
    @Volatile
    var mode: TrafficRoutingMode = TrafficRoutingMode.DEFAULT
        private set

    fun update(mode: TrafficRoutingMode) {
        this.mode = mode
    }
}
