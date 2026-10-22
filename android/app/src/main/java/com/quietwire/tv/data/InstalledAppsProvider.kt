package com.quietwire.tv.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class InstalledApp(
    val packageName: String,
    val label: String,
)

object InstalledAppsProvider {
    /** Launchable, non-system apps — the realistic split-tunnel candidate list on a TV box. */
    fun listUserFacingApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                    app.packageName != context.packageName
            }
            .map { app: ApplicationInfo ->
                InstalledApp(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
