package ru.coffeemaniavpn.app.deeplink

sealed class DeepLinkAction {
    data object Open : DeepLinkAction()

    data object Connect : DeepLinkAction()

    data object Disconnect : DeepLinkAction()

    data object Close : DeepLinkAction()

    data class Add(val url: String, val connectAfter: Boolean = false) : DeepLinkAction()

    data class Import(val payload: String) : DeepLinkAction()

    data class Routing(
        val profileJson: String,
        val enable: Boolean,
    ) : DeepLinkAction()

    /** Отправка сохранённой подписки на TV (локальный HTTP). */
    data class TvImport(
        val host: String,
        val port: Int,
        val token: String,
    ) : DeepLinkAction()
}

enum class DeepLinkEffect {
    None,
    RequestConnect,
    FinishActivity,
}
