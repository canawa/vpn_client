package ru.coffeemaniavpn.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import ru.coffeemaniavpn.app.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExporter {

    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    fun suggestedFileName(): String =
        "porozoff-logs-${fileNameFormat.format(Date())}.txt"

    fun buildExportText(): String = buildString {
        appendLine("=== POROZOFF VPN — export ===")
        appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("exported=${fileNameFormat.format(Date())}")
        appendLine("logFile=${AppLog.logPath()}")
        appendLine("crashFile=${AppLog.crashPath()}")
        appendLine()
        appendLine("--- app.log ---")
        append(AppLog.readFull().ifBlank { "(пусто)" })
        AppLog.readLastCrash()?.let { crash ->
            appendLine()
            appendLine("--- crash.log ---")
            append(crash)
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
