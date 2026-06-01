package online.coffemaniavpn.client.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import online.coffemaniavpn.client.ui.InstalledAppItem
import online.coffemaniavpn.client.util.AppLog

object InstalledAppsLoader {

    fun load(packageManager: PackageManager, ownPackageName: String): List<InstalledAppItem> {
        val byPackage = linkedMapOf<String, InstalledAppItem>()

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        runCatching {
            queryLauncherActivities(packageManager, launcherIntent).forEach { packageName ->
                addIfPresent(packageManager, byPackage, packageName, ownPackageName)
            }
        }.onFailure {
            AppLog.e("InstalledAppsLoader queryIntentActivities failed", it)
        }

        runCatching {
            installedApplications(packageManager).forEach { app ->
                if (app.packageName == ownPackageName || app.packageName in byPackage) return@forEach
                if (!shouldIncludeWithoutLauncher(app, packageManager)) return@forEach
                addIfPresent(packageManager, byPackage, app.packageName, ownPackageName)
            }
        }.onFailure {
            AppLog.e("InstalledAppsLoader getInstalledApplications failed", it)
        }

        val list = byPackage.values.sortedBy { it.label.lowercase() }
        AppLog.i("InstalledAppsLoader loaded ${list.size} apps")
        return list
    }

    private fun queryLauncherActivities(
        packageManager: PackageManager,
        intent: Intent,
    ): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(
                    (PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS).toLong(),
                ),
            ).map { it.activityInfo.packageName }
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS,
            ).map { it.activityInfo.packageName }
        }

    private fun shouldIncludeWithoutLauncher(app: ApplicationInfo, packageManager: PackageManager): Boolean {
        val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (!isUserApp && !isUpdatedSystem) return false
        return packageManager.checkPermission(
            android.Manifest.permission.INTERNET,
            app.packageName,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun addIfPresent(
        packageManager: PackageManager,
        target: MutableMap<String, InstalledAppItem>,
        packageName: String,
        ownPackageName: String,
    ) {
        if (packageName == ownPackageName || packageName in target) return
        runCatching {
            val info = getApplicationInfo(packageManager, packageName)
            if (info.packageName == ownPackageName) return
            val label = packageManager.getApplicationLabel(info).toString().ifBlank { packageName }
            target[packageName] = InstalledAppItem(packageName = packageName, label = label)
        }
    }

    private fun getApplicationInfo(packageManager: PackageManager, packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }

    private fun installedApplications(packageManager: PackageManager): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }
}
