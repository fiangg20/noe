package com.noe.app.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }

enum class SortOrder { Edited, Created, Title }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val seedIndex: Int = 0,
    val gridView: Boolean = true,
    val sortOrder: SortOrder = SortOrder.Edited,
    val userName: String = "",
)

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("noe_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = enumOf(prefs.getString("themeMode", null), ThemeMode.System),
        dynamicColor = prefs.getBoolean("dynamicColor", true),
        seedIndex = prefs.getInt("seedIndex", 0),
        gridView = prefs.getBoolean("gridView", true),
        sortOrder = enumOf(prefs.getString("sortOrder", null), SortOrder.Edited),
        userName = prefs.getString("userName", "") ?: "",
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("themeMode", s.themeMode.name)
            .putBoolean("dynamicColor", s.dynamicColor)
            .putInt("seedIndex", s.seedIndex)
            .putBoolean("gridView", s.gridView)
            .putString("sortOrder", s.sortOrder.name)
            .putString("userName", s.userName)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
