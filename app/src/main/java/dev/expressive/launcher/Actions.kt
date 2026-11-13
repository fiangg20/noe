package dev.expressive.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.launchApp(app: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(if (app.isTv) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.packageName, app.activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { startActivity(intent) }
}

fun Context.openSystemSettings(action: String = Settings.ACTION_SETTINGS) {
    val candidates = listOf(
        Intent(action),
        Intent(Settings.ACTION_SETTINGS),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings"),
        Intent().setClassName("com.android.settings", "com.android.settings.Settings")
    )
    for (candidate in candidates) {
        val started = runCatching {
            startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (started) return
    }
    val packages = listOf("com.android.tv.settings", "com.google.android.tvsettings", "com.android.settings")
    for (pkg in packages) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            val started = runCatching {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (started) return
        }
    }
    toast("Settings app not found on this device")
}

fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

fun Context.uninstallApp(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
