package ru.coffeemaniavpn.app.util

import android.content.ContentValues
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.TrafficRoutingStore
import ru.coffeemaniavpn.app.vpn.KillSwitchVpnService
import ru.coffeemaniavpn.app.vpn.VpnAutoReconnect
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.XrayCoreManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExporter {

    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    fun suggestedFileName(): String =
        "clevvpn-logs-${fileNameFormat.format(Date())}.txt"

    fun buildExportText(): String = buildString {
        appendLine("=== ClevVPN — diagnostic export ===")
        appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("exported=${fileNameFormat.format(Date())}")
        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("android=${Build.VERSION.RELEASE} (sdk=${Build.VERSION.SDK_INT})")
        appendLine("logFile=${AppLog.logPath()}")
        appendLine("crashFile=${AppLog.crashPath()}")
        appendLine()
        appendLine("--- vpn snapshot ---")
        appendLine("status=${VpnManager.status.value}")
        appendLine("xrayRunning=${XrayCoreManager.isRunning()}")
        appendLine("killSwitchActive=${KillSwitchVpnService.isActive}")
        appendLine("lastError=${VpnManager.lastError.value ?: "—"}")
        appendLine("node=${VpnAutoReconnect.connectedNode()?.name ?: "—"}")
        val settings = ConnectionSettingsStore.state
        appendLine("routing=${TrafficRoutingStore.mode}")
        appendLine("appsEnabled=${settings.appsEnabled} mode=${settings.appsMode} count=${settings.appPackages.size}")
        appendLine("killSwitch=${settings.killSwitchEnabled}")
        appendLine()
        appendLine("--- app.log ---")
        append(AppLog.readFull().ifBlank { "(пусто)" })
        AppLog.readLastCrash()?.let { crash ->
            appendLine()
            appendLine("--- crash.log ---")
            append(crash)
        }
    }

    fun createShareIntent(context: Context): Intent {
        AppLog.i("LogExporter share requested")
        val fileName = suggestedFileName()
        val exportDir = File(context.cacheDir, "export_logs").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(buildExportText(), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ClevVPN logs")
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun saveToDownloads(context: Context): Result<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.failure(
                UnsupportedOperationException("Требуется выбор места сохранения"),
            )
        }
        val resolver = context.contentResolver
        val fileName = suggestedFileName()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IllegalStateException("Не удалось создать файл"))
        return runCatching {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(buildExportText().toByteArray(Charsets.UTF_8))
            } ?: error("Не удалось записать файл")
            val published = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, published, null, null)
            fileName
        }.onFailure {
            runCatching { resolver.delete(uri, null, null) }
        }
    }

    fun writeToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(buildExportText().toByteArray(Charsets.UTF_8))
        } ?: error("Не удалось записать файл")
    }
}
