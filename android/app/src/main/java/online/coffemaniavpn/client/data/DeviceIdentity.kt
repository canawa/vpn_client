package online.coffemaniavpn.client.data

import android.content.Context
import android.os.Build
import online.coffemaniavpn.client.BuildConfig
import online.coffemaniavpn.client.util.AppLog
import java.util.UUID

object DeviceIdentity {
    private const val PREFS_NAME = "device_identity"
    private const val KEY_HWID = "hwid"

    fun hwid(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_HWID, null)?.let { return it }

        val value = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_HWID, value).apply()
        AppLog.i("DeviceIdentity created hwid=$value")
        return value
    }

    fun subscriptionHeaders(context: Context): Map<String, String> {
        val id = hwid(context)
        val appVersion = BuildConfig.VERSION_NAME.substringBefore('-').ifBlank { "1.8.29" }
        return mapOf(
            "x-hwid" to id,
            "X-HWID" to id,
            "x-device-os" to "Android",
            "x-ver-os" to Build.VERSION.RELEASE,
            "x-device-model" to Build.MODEL.take(64),
            "User-Agent" to "v2rayNG/$appVersion",
        )
    }
}
