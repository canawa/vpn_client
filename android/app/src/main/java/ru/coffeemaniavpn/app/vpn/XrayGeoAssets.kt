package ru.coffeemaniavpn.app.vpn

import android.content.Context
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.util.AppLog
import java.io.File

internal object XrayGeoAssets {
    private const val PREFS = "xray_geo_assets"
    private const val KEY_VERSION = "copied_version_code"

    private val ASSET_FILES = listOf(
        "geoip.dat",
        "geosite.dat",
        "geoip-only-cn-private.dat",
    )

    fun ensureInstalled(context: Context): File {
        val targetDir = context.filesDir
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val copiedVersion = prefs.getInt(KEY_VERSION, -1)
        val forceRefresh = copiedVersion != BuildConfig.VERSION_CODE

        ASSET_FILES.forEach { name ->
            copyAssetIfNeeded(context, name, targetDir, forceRefresh)
        }

        if (forceRefresh) {
            prefs.edit().putInt(KEY_VERSION, BuildConfig.VERSION_CODE).apply()
        }

        val geoip = File(targetDir, "geoip.dat")
        val geosite = File(targetDir, "geosite.dat")
        AppLog.i(
            "XrayGeoAssets ready geoip=${geoip.length()} geosite=${geosite.length()} " +
                "forceRefresh=$forceRefresh dir=${targetDir.absolutePath}",
        )
        return targetDir
    }

    private fun copyAssetIfNeeded(
        context: Context,
        name: String,
        targetDir: File,
        forceRefresh: Boolean,
    ) {
        val target = File(targetDir, name)
        if (!forceRefresh && target.exists() && target.length() > 0) return

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
