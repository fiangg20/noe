package dev.expressive.launcher.ui

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat

fun launcherFontFamily(context: Context): FontFamily {
    val id = context.resources.getIdentifier("google_sans_flex", "font", context.packageName)
    if (id == 0) return FontFamily.Default
    val usable = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() != null
    if (!usable) return FontFamily.Default
    return FontFamily(
        Font(id, FontWeight.Normal),
        Font(id, FontWeight.Medium),
        Font(id, FontWeight.SemiBold),
        Font(id, FontWeight.Bold),
        Font(id, FontWeight.ExtraBold),
        Font(id, FontWeight.Black)
    )
}

fun Typography.withFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family)
)
