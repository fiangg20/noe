package dev.expressive.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Accent(
    val name: String,
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color
)

val Accents = listOf(
    Accent("Violet", Color(0xFFCABEFF), Color(0xFF2F1B7A), Color(0xFF473A92), Color(0xFFE7DEFF)),
    Accent("Mint", Color(0xFF6FDBC3), Color(0xFF00382E), Color(0xFF0F5043), Color(0xFF8CF8DF)),
    Accent("Rose", Color(0xFFFFB0CB), Color(0xFF5E1136), Color(0xFF7B2A4D), Color(0xFFFFD9E4)),
    Accent("Amber", Color(0xFFFFB95C), Color(0xFF472A00), Color(0xFF653E00), Color(0xFFFFDDB1)),
    Accent("Sky", Color(0xFF9FCAFF), Color(0xFF00315C), Color(0xFF00497F), Color(0xFFD2E4FF))
)

private val BaseDark = darkColorScheme(
    secondary = Color(0xFF6FDBC3),
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF0F5043),
    onSecondaryContainer = Color(0xFF8CF8DF),
    tertiary = Color(0xFFFFB0CB),
    onTertiary = Color(0xFF5E1136),
    tertiaryContainer = Color(0xFF7B2A4D),
    onTertiaryContainer = Color(0xFFFFD9E4),
    background = Color(0xFF0F0E15),
    onBackground = Color(0xFFE8E3EE),
    surface = Color(0xFF0F0E15),
    onSurface = Color(0xFFE8E3EE),
    surfaceVariant = Color(0xFF47454F),
    onSurfaceVariant = Color(0xFFCAC5D2),
    surfaceDim = Color(0xFF0F0E15),
    surfaceBright = Color(0xFF353240),
    surfaceContainerLowest = Color(0xFF0A090F),
    surfaceContainerLow = Color(0xFF181620),
    surfaceContainer = Color(0xFF1C1A25),
    surfaceContainerHigh = Color(0xFF272431),
    surfaceContainerHighest = Color(0xFF322F3C),
    outline = Color(0xFF948F9D),
    outlineVariant = Color(0xFF47454F)
)

private val LauncherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

private val LauncherTypography = Typography(
    displayLarge = TextStyle(fontSize = 120.sp, lineHeight = 112.sp, fontWeight = FontWeight.Black, letterSpacing = (-5).sp),
    displayMedium = TextStyle(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
)

@Composable
fun LauncherTheme(accentIndex: Int, content: @Composable () -> Unit) {
    val accent = Accents.getOrElse(accentIndex) { Accents.first() }
    val scheme = remember(accent) {
        BaseDark.copy(
            primary = accent.primary,
            onPrimary = accent.onPrimary,
            primaryContainer = accent.container,
            onPrimaryContainer = accent.onContainer
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = LauncherShapes,
        typography = LauncherTypography,
        content = content
    )
}
