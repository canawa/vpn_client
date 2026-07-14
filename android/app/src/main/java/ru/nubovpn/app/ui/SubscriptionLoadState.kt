package ru.nubovpn.app.ui

data class SubscriptionLoadState(
    val active: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
)
