package com.noe.app.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import java.io.BufferedInputStream
import java.io.DataInputStream
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * Google Sans Flex, a variable font. Put the file at
 * app/src/main/res/font/google_sans_flex.ttf and it is picked up automatically.
 * If the file is missing, the app falls back to the system font instead of failing to build.
 */
fun noeFontFamily(context: Context): FontFamily {
    val resId = context.resources.getIdentifier("google_sans_flex", "font", context.packageName)
    if (resId == 0) return FontFamily.Default

    // A static (non-variable) font file cannot change weight, so give Compose a single
    // Normal entry and let it draw bold and italic itself. A variable font gets real weights.
    if (!isVariableFont(context, resId)) {
        return FontFamily(Font(resId = resId, weight = FontWeight.Normal, style = FontStyle.Normal))
    }

    fun weighted(weight: FontWeight) = Font(
        resId = resId,
        weight = weight,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
    )

    return FontFamily(
        weighted(FontWeight.Normal),
        weighted(FontWeight.Medium),
        weighted(FontWeight.SemiBold),
        weighted(FontWeight.Bold),
    )
}

/** True when the font file has an 'fvar' table, which is what makes a font variable. */
private fun isVariableFont(context: Context, resId: Int): Boolean = runCatching {
    DataInputStream(BufferedInputStream(context.resources.openRawResource(resId))).use { input ->
        val header = ByteArray(12)
        input.readFully(header)
        val tables = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
        val record = ByteArray(16)
        var found = false
        for (i in 0 until tables) {
            input.readFully(record)
            if (record[0] == 'f'.code.toByte() && record[1] == 'v'.code.toByte() &&
                record[2] == 'a'.code.toByte() && record[3] == 'r'.code.toByte()
            ) {
                found = true
                break
            }
        }
        found
    }
}.getOrDefault(false)

/** Expressive type: the chosen font everywhere, with heavier weights on headlines and titles. */
fun noeTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.styled(weight: FontWeight? = null): TextStyle =
        copy(fontFamily = family, fontWeight = weight ?: fontWeight)

    return base.copy(
        displayLarge = base.displayLarge.styled(FontWeight.Bold),
        displayMedium = base.displayMedium.styled(FontWeight.Bold),
        displaySmall = base.displaySmall.styled(FontWeight.Bold),
        headlineLarge = base.headlineLarge.styled(FontWeight.Bold),
        headlineMedium = base.headlineMedium.styled(FontWeight.Bold),
        headlineSmall = base.headlineSmall.styled(FontWeight.SemiBold),
        titleLarge = base.titleLarge.styled(FontWeight.SemiBold),
        titleMedium = base.titleMedium.styled(FontWeight.SemiBold),
        titleSmall = base.titleSmall.styled(FontWeight.Medium),
        bodyLarge = base.bodyLarge.styled(),
        bodyMedium = base.bodyMedium.styled(),
        bodySmall = base.bodySmall.styled(),
        labelLarge = base.labelLarge.styled(FontWeight.SemiBold),
        labelMedium = base.labelMedium.styled(FontWeight.Medium),
        labelSmall = base.labelSmall.styled(FontWeight.Medium),
    )
}
