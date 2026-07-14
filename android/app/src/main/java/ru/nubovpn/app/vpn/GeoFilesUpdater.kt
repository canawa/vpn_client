package ru.nubovpn.app.vpn

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.nubovpn.app.util.AppLog
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Скачивает гео-файлы маршрутизации (geoip.dat / geosite.dat) — как это делает Happ.
 * Файлы кладутся в filesDir, откуда их читает xray-core (XrayCoreManager.init).
 */
object GeoFilesUpdater {

    data class GeoFile(val name: String, val urls: List<String>)

    private val FILES = listOf(
        GeoFile(
            name = "geoip.dat",
            urls = listOf(
                "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat",
                "https://raw.githubusercontent.com/Loyalsoldier/v2ray-rules-dat/release/geoip.dat",
                "https://cdn.jsdelivr.net/gh/Loyalsoldier/v2ray-rules-dat@release/geoip.dat",
            ),
        ),
        GeoFile(
            name = "geosite.dat",
            urls = listOf(
                "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat",
                "https://raw.githubusercontent.com/Loyalsoldier/v2ray-rules-dat/release/geosite.dat",
                "https://cdn.jsdelivr.net/gh/Loyalsoldier/v2ray-rules-dat@release/geosite.dat",
            ),
        ),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    fun isInstalled(context: Context): Boolean =
        FILES.all { File(context.filesDir, it.name).let { f -> f.exists() && f.length() > 0 } }

    /** Время последнего обновления (мс с эпохи) или null, если файлы не установлены. */
    fun lastUpdatedAt(context: Context): Long? {
        if (!isInstalled(context)) return null
        return FILES.minOf { File(context.filesDir, it.name).lastModified() }
    }

    /**
     * Скачивает гео-файлы. [onProgress] получает имя файла и прогресс 0..1 суммарно.
     * Возвращает суммарный размер загруженных файлов в байтах.
     */
    suspend fun update(
        context: Context,
        onProgress: (message: String, progress: Float) -> Unit = { _, _ -> },
    ): Long = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        FILES.forEachIndexed { index, geoFile ->
            val base = index.toFloat() / FILES.size
            val span = 1f / FILES.size
            onProgress("Загрузка ${geoFile.name}…", base)
            val bytes = downloadWithFallback(context, geoFile) { fileProgress ->
                onProgress("Загрузка ${geoFile.name}…", base + span * fileProgress)
            }
            totalBytes += bytes
        }
        onProgress("Готово", 1f)
        AppLog.i("GeoFilesUpdater updated total=${totalBytes / 1024} KB")
        totalBytes
    }

    /** Тихая установка при первом запуске, если файлов ещё нет. */
    suspend fun ensureInstalledSilently(context: Context) {
        if (isInstalled(context)) return
        runCatching { update(context) }
            .onSuccess { AppLog.i("GeoFilesUpdater initial install ok") }
            .onFailure { AppLog.w("GeoFilesUpdater initial install failed", it) }
    }

    private fun downloadWithFallback(
        context: Context,
        geoFile: GeoFile,
        onProgress: (Float) -> Unit,
    ): Long {
        var lastError: Exception? = null
        geoFile.urls.forEach { url ->
            try {
                return downloadTo(context, url, geoFile.name, onProgress)
            } catch (e: Exception) {
                lastError = e
                AppLog.w("GeoFilesUpdater ${geoFile.name} failed from $url: ${e.message}")
            }
        }
        throw IllegalStateException(
            "Не удалось скачать ${geoFile.name} — проверьте интернет",
            lastError,
        )
    }

    private fun downloadTo(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): Long {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NuboVPN/Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Пустой ответ")
            val contentLength = body.contentLength()

            val tmp = File(context.filesDir, "$fileName.tmp")
            var written = 0L
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        if (contentLength > 0) {
                            onProgress((written.toFloat() / contentLength).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (written <= 0) {
                tmp.delete()
                error("Файл пуст")
            }

            val target = File(context.filesDir, fileName)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            AppLog.i("GeoFilesUpdater saved $fileName size=${written / 1024} KB from $url")
            return written
        }
    }
}
