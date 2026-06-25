package ru.coffeemaniavpn.app.vpn

import android.content.Context
import ru.coffeemaniavpn.app.util.AppLog
import java.io.File

internal object XrayGeoAssets {
    private val ASSET_FILES = listOf(
        "geoip.dat",
        "geosite.dat",
        "geoip-only-cn-private.dat",
    )

    fun ensureInstalled(context: Context): File {
        val targetDir = context.filesDir
        ASSET_FILES.forEach { name ->
            copyAssetIfNeeded(context, name, targetDir)
        }
        return targetDir
    }

    private fun copyAssetIfNeeded(context: Context, name: String, targetDir: File) {
        val target = File(targetDir, name)
        if (target.exists() && target.length() > 0) return

        runCatching {
            context.assets.open(name).use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            AppLog.i("XrayGeoAssets copied $name size=${target.length()}")
        }.onFailure {
            AppLog.w("XrayGeoAssets missing asset $name", it)
        }
    }
}
