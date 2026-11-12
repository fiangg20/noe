package com.noe.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Converts HSL (hue 0-360, saturation 0-1, lightness 0-1) to a Compose color. */
fun hsl(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        (r + m).coerceIn(0f, 1f),
        (g + m).coerceIn(0f, 1f),
        (b + m).coerceIn(0f, 1f),
    )
}

class SeedPalette(val name: String, val hue: Float)

val SeedPalettes = listOf(
    SeedPalette("Violet", 265f),
    SeedPalette("Ocean", 205f),
    SeedPalette("Forest", 145f),
    SeedPalette("Sunset", 18f),
    SeedPalette("Rose", 338f),
    SeedPalette("Gold", 42f),
)

fun lightSchemeFor(hue: Float): ColorScheme {
    val tert = hue + 60f
    val primary = hsl(hue, 0.55f, 0.40f)
    return lightColorScheme(
        primary = primary,
        onPrimary = hsl(hue, 0.20f, 0.98f),
        primaryContainer = hsl(hue, 0.85f, 0.90f),
        onPrimaryContainer = hsl(hue, 0.60f, 0.15f),
        inversePrimary = hsl(hue, 0.80f, 0.80f),
        secondary = hsl(hue, 0.25f, 0.40f),
        onSecondary = hsl(hue, 0.20f, 0.98f),
        secondaryContainer = hsl(hue, 0.40f, 0.88f),
        onSecondaryContainer = hsl(hue, 0.35f, 0.15f),
        tertiary = hsl(tert, 0.35f, 0.40f),
        onTertiary = hsl(tert, 0.20f, 0.98f),
        tertiaryContainer = hsl(tert, 0.55f, 0.89f),
        onTertiaryContainer = hsl(tert, 0.40f, 0.15f),
        background = hsl(hue, 0.40f, 0.985f),
        onBackground = hsl(hue, 0.15f, 0.10f),
        surface = hsl(hue, 0.40f, 0.985f),
        onSurface = hsl(hue, 0.15f, 0.10f),
        surfaceVariant = hsl(hue, 0.28f, 0.90f),
        onSurfaceVariant = hsl(hue, 0.15f, 0.30f),
        surfaceTint = primary,
        inverseSurface = hsl(hue, 0.10f, 0.20f),
        inverseOnSurface = hsl(hue, 0.20f, 0.95f),
        outline = hsl(hue, 0.10f, 0.50f),
        outlineVariant = hsl(hue, 0.18f, 0.80f),
        surfaceBright = hsl(hue, 0.40f, 0.985f),
        surfaceDim = hsl(hue, 0.20f, 0.87f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = hsl(hue, 0.38f, 0.96f),
        surfaceContainer = hsl(hue, 0.34f, 0.94f),
        surfaceContainerHigh = hsl(hue, 0.30f, 0.92f),
        surfaceContainerHighest = hsl(hue, 0.28f, 0.90f),
    )
}

fun darkSchemeFor(hue: Float): ColorScheme {
    val tert = hue + 60f
    val primary = hsl(hue, 0.85f, 0.80f)
    return darkColorScheme(
        primary = primary,
        onPrimary = hsl(hue, 0.60f, 0.20f),
        primaryContainer = hsl(hue, 0.50f, 0.30f),
        onPrimaryContainer = hsl(hue, 0.85f, 0.90f),
        inversePrimary = hsl(hue, 0.55f, 0.40f),
        secondary = hsl(hue, 0.40f, 0.80f),
        onSecondary = hsl(hue, 0.35f, 0.20f),
        secondaryContainer = hsl(hue, 0.25f, 0.30f),
        onSecondaryContainer = hsl(hue, 0.40f, 0.90f),
        tertiary = hsl(tert, 0.55f, 0.80f),
        onTertiary = hsl(tert, 0.40f, 0.20f),
        tertiaryContainer = hsl(tert, 0.30f, 0.30f),
        onTertiaryContainer = hsl(tert, 0.55f, 0.90f),
        background = hsl(hue, 0.15f, 0.08f),
        onBackground = hsl(hue, 0.15f, 0.90f),
        surface = hsl(hue, 0.15f, 0.08f),
        onSurface = hsl(hue, 0.15f, 0.90f),
        surfaceVariant = hsl(hue, 0.12f, 0.27f),
        onSurfaceVariant = hsl(hue, 0.15f, 0.80f),
        surfaceTint = primary,
        inverseSurface = hsl(hue, 0.15f, 0.90f),
        inverseOnSurface = hsl(hue, 0.10f, 0.20f),
        outline = hsl(hue, 0.08f, 0.60f),
        outlineVariant = hsl(hue, 0.10f, 0.30f),
        surfaceBright = hsl(hue, 0.12f, 0.24f),
        surfaceDim = hsl(hue, 0.15f, 0.08f),
        surfaceContainerLowest = hsl(hue, 0.15f, 0.05f),
        surfaceContainerLow = hsl(hue, 0.14f, 0.10f),
        surfaceContainer = hsl(hue, 0.13f, 0.13f),
        surfaceContainerHigh = hsl(hue, 0.12f, 0.17f),
        surfaceContainerHighest = hsl(hue, 0.11f, 0.21f),
    )
}

/** Card colors a note can have. Index 0 means "no color" (uses the theme surface). */
class NoteColor(val name: String, val hue: Float)

val NoteColors = listOf(
    NoteColor("Default", -1f),
    NoteColor("Coral", 6f),
    NoteColor("Peach", 28f),
    NoteColor("Sand", 48f),
    NoteColor("Mint", 100f),
    NoteColor("Sage", 160f),
    NoteColor("Sky", 200f),
    NoteColor("Periwinkle", 235f),
    NoteColor("Lilac", 280f),
    NoteColor("Blossom", 330f),
)

fun noteContainerColor(index: Int, dark: Boolean, fallback: Color): Color {
    val c = NoteColors.getOrNull(index) ?: return fallback
    if (c.hue < 0f) return fallback
    return if (dark) hsl(c.hue, 0.30f, 0.22f) else hsl(c.hue, 0.75f, 0.89f)
}

fun noteContentColor(index: Int, dark: Boolean, fallback: Color): Color {
    val c = NoteColors.getOrNull(index) ?: return fallback
    if (c.hue < 0f) return fallback
    return if (dark) hsl(c.hue, 0.25f, 0.92f) else hsl(c.hue, 0.40f, 0.12f)
}
