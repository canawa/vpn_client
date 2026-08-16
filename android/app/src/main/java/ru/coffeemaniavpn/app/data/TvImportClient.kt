package ru.coffeemaniavpn.app.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.coffeemaniavpn.app.util.AppLog
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TvImportSubmitResult {
    data object Success : TvImportSubmitResult()
    data class RejectedUrl(val code: Int) : TvImportSubmitResult()
    data class Forbidden(val code: Int) : TvImportSubmitResult()
    data class HttpError(val code: Int) : TvImportSubmitResult()
    data object NetworkError : TvImportSubmitResult()
}

/**
 * POST /submit на локальный сервер TV.
 * Content-Type: application/x-www-form-urlencoded
 * Body: token=&url=
 */
object TvImportClient {
    private const val TIMEOUT_SEC = 6L
    private const val PATH = "/submit"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .callTimeout(TIMEOUT_SEC + 2, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    fun submit(
        host: String,
        port: Int,
        token: String,
        subscriptionUrl: String,
        retryOnce: Boolean = true,
    ): TvImportSubmitResult {
        val url = "http://$host:$port$PATH"
        AppLog.i("TvImportClient POST $url tokenLen=${token.length} urlLen=${subscriptionUrl.length}")
        return try {
            executeOnce(url, token, subscriptionUrl)
        } catch (e: IOException) {
            AppLog.w("TvImportClient IOException: ${e.message}")
            if (!retryOnce) return TvImportSubmitResult.NetworkError
            return try {
                AppLog.i("TvImportClient retry once")
                executeOnce(url, token, subscriptionUrl)
            } catch (e2: IOException) {
                AppLog.w("TvImportClient retry failed: ${e2.message}")
                TvImportSubmitResult.NetworkError
            }
        } catch (t: Throwable) {
            AppLog.e("TvImportClient unexpected", t)
            TvImportSubmitResult.NetworkError
        }
    }

    private fun executeOnce(
        endpoint: String,
        token: String,
        subscriptionUrl: String,
    ): TvImportSubmitResult {
        val body = FormBody.Builder()
            .add("token", token)
            .add("url", subscriptionUrl)
            .build()
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val code = response.code
            AppLog.i("TvImportClient response code=$code")
            return when (code) {
                200 -> TvImportSubmitResult.Success
                400 -> TvImportSubmitResult.RejectedUrl(code)
                403 -> TvImportSubmitResult.Forbidden(code)
                else -> TvImportSubmitResult.HttpError(code)
            }
        }
    }
}
