package ru.coffeemaniavpn.app.ui

/**
 * Экран отправки подписки на TV по диплинку clevvpn://tv-import.
 */
data class TvImportTarget(
    val host: String,
    val port: Int,
    val token: String,
)

sealed class TvImportUiState {
    data object Hidden : TvImportUiState()

    data class Sending(val target: TvImportTarget) : TvImportUiState()

    data class Success(val target: TvImportTarget) : TvImportUiState()

    data class NoSubscription(
        val target: TvImportTarget,
        val draftUrl: String = "",
    ) : TvImportUiState()

    data class Error(
        val target: TvImportTarget?,
        val message: String,
        val allowManualUrl: Boolean = false,
        val draftUrl: String = "",
    ) : TvImportUiState()
}
