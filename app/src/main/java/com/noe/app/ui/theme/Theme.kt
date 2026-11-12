package com.noe.app.ui.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noe.app.data.AppSettings
import com.noe.app.data.SettingChoices
import com.noe.app.data.ThemeMode

/** Expressive shapes: generous, rounder corners than the baseline. */
val NoeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

/** Look-and-feel choices from Settings that individual components read. */
class NoeStyle(val cardCorner: Dp, val previewLines: Int)

val LocalNoeStyle = staticCompositionLocalOf { NoeStyle(28.dp, 10) }

@Composable
fun NoeTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val typography = remember(context) { noeTypography(noeFontFamily(context)) }

    val baseScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val hue = SeedPalettes.getOrElse(settings.seedIndex) { SeedPalettes[0] }.hue
            if (dark) darkSchemeFor(hue) else lightSchemeFor(hue)
        }
    }

    // Pure black surfaces for OLED screens.
    val colorScheme = if (dark && settings.amoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0B0B0B),
            surfaceContainer = Color(0xFF131313),
            surfaceContainerHigh = Color(0xFF1B1B1B),
            surfaceContainerHighest = Color(0xFF242424),
        )
    } else {
        baseScheme
    }

    val density = LocalDensity.current
    val textScale = SettingChoices.textSizeScales.getOrElse(settings.textSizeIndex) { 1f }
    val style = NoeStyle(
        cardCorner = SettingChoices.cornerDp.getOrElse(settings.cornerIndex) { 28 }.dp,
        previewLines = SettingChoices.previewLines.getOrElse(settings.previewIndex) { 10 },
    )

    // Keep the status bar and navigation bar icons readable in the chosen theme.
    val activity = context as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
    }

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * textScale),
        LocalNoeStyle provides style,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = NoeShapes,
            typography = typography,
            content = content,
        )
    }
}
