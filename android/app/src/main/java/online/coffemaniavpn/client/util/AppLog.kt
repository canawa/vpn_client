package online.coffemaniavpn.client.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val TAG = "CoffemaniaVPN"
    private const val MAX_FILE_BYTES = 512 * 1024

    private val lock = Any()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var logFile: File? = null
    private var crashFile: File? = null

    fun init(context: Context) {
        val logsDir = File(context.getExternalFilesDir(null), "logs").apply { mkdirs() }
        logFile = File(logsDir, "app.log")
        crashFile = File(logsDir, "crash.log")
        i("AppLog init, path=${logFile?.absolutePath}")
    }

    fun installCrashHandler(defaultHandler: Thread.UncaughtExceptionHandler?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("FATAL uncaught on thread=${thread.name}", throwable)
            writeCrashReport(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        i("Crash handler installed")
    }

    fun i(message: String) = write("I", message, null)

    fun w(message: String, throwable: Throwable? = null) = write("W", message, throwable)

    fun e(message: String, throwable: Throwable? = null) = write("E", message, throwable)

    fun logPath(): String = logFile?.absolutePath ?: "n/a"

    fun crashPath(): String = crashFile?.absolutePath ?: "n/a"

    fun readTail(maxLines: Int = 300): String {
        val file = logFile ?: return "Лог-файл не инициализирован"
        if (!file.exists() || file.length() == 0L) return "Лог пока пуст"
        return runCatching {
            file.readLines().takeLast(maxLines).joinToString("\n")
        }.getOrElse { "Не удалось прочитать лог: ${it.message}" }
    }

    fun readLastCrash(): String? {
        val file = crashFile ?: return null
        if (!file.exists() || file.length() == 0L) return null
        return runCatching { file.readText() }.getOrNull()
    }

    private fun write(level: String, message: String, throwable: Throwable?) {
        val timestamp = timeFormat.format(Date())
        val line = buildString {
            append(timestamp)
            append(" [")
            append(level)
            append("] ")
            append(message)
            throwable?.let {
                append('\n')
                append(Log.getStackTraceString(it))
            }
        }

        when (level) {
            "E" -> Log.e(TAG, message, throwable)
            "W" -> Log.w(TAG, message, throwable)
            else -> Log.i(TAG, message, throwable)
        }

        synchronized(lock) {
            runCatching {
                val file = logFile ?: return@runCatching
                trimIfNeeded(file)
                file.appendText(line + "\n")
            }.onFailure {
                Log.e(TAG, "Failed to write log file", it)
            }
        }
    }

    private fun writeCrashReport(thread: Thread, throwable: Throwable) {
        synchronized(lock) {
            runCatching {
                val file = crashFile ?: return@runCatching
                val report = buildString {
                    append("time=")
                    append(timeFormat.format(Date()))
                    append("\nthread=")
                    append(thread.name)
                    append("\n")
                    append(Log.getStackTraceString(throwable))
                    append("\n\n--- recent log ---\n")
                    append(readTail(80))
                }
                file.writeText(report)
            }
        }
    }

    private fun trimIfNeeded(file: File) {
        if (file.length() <= MAX_FILE_BYTES) return
        val lines = file.readLines()
        file.writeText(lines.takeLast(1500).joinToString("\n") + "\n")
    }
}
