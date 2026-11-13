package dev.expressive.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CookieShape(
    private val sides: Int,
    private val amplitude: Float,
    private val phase: Float = 0f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = min(cx, cy) / (1f + amplitude)
        val steps = 96
        val path = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps * 2f * PI.toFloat()
            val r = base * (1f + amplitude * cos(sides * t + phase))
            val x = cx + r * cos(t)
            val y = cy + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

typealias ShapeAt = (Float, Density) -> Shape

fun roundedMorph(idle: Dp, focused: Dp): ShapeAt = { progress, density ->
    with(density) {
        RoundedCornerShape(lerp(idle.toPx(), focused.toPx(), progress).coerceAtLeast(0f))
    }
}

fun cookieMorph(sides: Int, idleAmplitude: Float, focusedAmplitude: Float, turn: Float): ShapeAt = { progress, _ ->
    CookieShape(sides, lerp(idleAmplitude, focusedAmplitude, progress), turn * progress)
}

fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}
