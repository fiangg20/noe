package com.noe.app.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }

enum class SortOrder { Edited, Created, Title }

/** Choices for the customization options. Settings store the index into these lists. */
object SettingChoices {
    val textSizeLabels = listOf("Small", "Default", "Large", "Huge")
    val textSizeScales = listOf(0.9f, 1f, 1.15f, 1.3f)

    val cornerLabels = listOf("Sharp", "Soft", "Round", "Pill")
    val cornerDp = listOf(12, 20, 28, 40)

    val columnLabels = listOf("Auto", "2", "3")

    val previewLabels = listOf("Short", "Medium", "Long")
    val previewLines = listOf(4, 10, 24)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val seedIndex: Int = 0,
    val gridView: Boolean = true,
    val sortOrder: SortOrder = SortOrder.Edited,
    val amoled: Boolean = false,
    val textSizeIndex: Int = 1,
    val cornerIndex: Int = 2,
    val columnsIndex: Int = 0,
    val previewIndex: Int = 1,
    val defaultColor: Int = 0,
    val showStats: Boolean = true,
)

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("noe_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = enumOf(prefs.getString("themeMode", null), ThemeMode.System),
        dynamicColor = prefs.getBoolean("dynamicColor", true),
        seedIndex = prefs.getInt("seedIndex", 0),
        gridView = prefs.getBoolean("gridView", true),
        sortOrder = enumOf(prefs.getString("sortOrder", null), SortOrder.Edited),
        amoled = prefs.getBoolean("amoled", false),
        textSizeIndex = prefs.getInt("textSizeIndex", 1),
        cornerIndex = prefs.getInt("cornerIndex", 2),
        columnsIndex = prefs.getInt("columnsIndex", 0),
        previewIndex = prefs.getInt("previewIndex", 1),
        defaultColor = prefs.getInt("defaultColor", 0),
        showStats = prefs.getBoolean("showStats", true),
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("themeMode", s.themeMode.name)
            .putBoolean("dynamicColor", s.dynamicColor)
            .putInt("seedIndex", s.seedIndex)
            .putBoolean("gridView", s.gridView)
            .putString("sortOrder", s.sortOrder.name)
            .putBoolean("amoled", s.amoled)
            .putInt("textSizeIndex", s.textSizeIndex)
            .putInt("cornerIndex", s.cornerIndex)
            .putInt("columnsIndex", s.columnsIndex)
            .putInt("previewIndex", s.previewIndex)
            .putInt("defaultColor", s.defaultColor)
            .putBoolean("showStats", s.showStats)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
