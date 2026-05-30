package online.coffemaniavpn.client.deeplink

sealed class DeepLinkAction {
    data object Open : DeepLinkAction()

    data object Connect : DeepLinkAction()

    data object Disconnect : DeepLinkAction()

    data object Close : DeepLinkAction()

    data class Add(val url: String) : DeepLinkAction()

    data class Import(val payload: String) : DeepLinkAction()

    data class Routing(
        val profileJson: String,
        val enable: Boolean,
    ) : DeepLinkAction()
}

enum class DeepLinkEffect {
    None,
    RequestConnect,
    FinishActivity,
}
