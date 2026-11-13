package dev.expressive.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Immutable
data class Blob(val x: Float, val y: Float, val radius: Float, val color: Color)

@Immutable
data class WallpaperPreset(
    val id: String,
    val name: String,
    val base: Color,
    val blobs: List<Blob>,
    val animated: Boolean
)

val WallpaperPresets = listOf(
    WallpaperPreset(
        id = "aurora",
        name = "Aurora",
        base = Color(0xFF0B0A14),
        blobs = listOf(
            Blob(0.2f, 0.2f, 0.7f, Color(0xFF6B4EFF)),
            Blob(0.8f, 0.35f, 0.6f, Color(0xFF1FD1B0)),
            Blob(0.55f, 0.95f, 0.6f, Color(0xFFFF5C93))
        ),
        animated = true
    ),
    WallpaperPreset(
        id = "dusk",
        name = "Dusk",
        base = Color(0xFF14101F),
        blobs = listOf(
            Blob(0.85f, 0.1f, 0.8f, Color(0xFFFF8A3D)),
            Blob(0.3f, 0.75f, 0.8f, Color(0xFFB5179E))
        ),
        animated = false
    ),
    WallpaperPreset(
        id = "lagoon",
        name = "Lagoon",
        base = Color(0xFF061A1F),
        blobs = listOf(
            Blob(0.15f, 0.85f, 0.8f, Color(0xFF00C2A8)),
            Blob(0.9f, 0.2f, 0.7f, Color(0xFF2D6BFF))
        ),
        animated = false
    ),
    WallpaperPreset(
        id = "ember",
        name = "Ember",
        base = Color(0xFF1A0B0B),
        blobs = listOf(
            Blob(0.8f, 0.85f, 0.8f, Color(0xFFFF4D3D)),
            Blob(0.2f, 0.15f, 0.6f, Color(0xFFFFB02E))
        ),
        animated = false
    ),
    WallpaperPreset(
        id = "orchid",
        name = "Orchid",
        base = Color(0xFF150B1A),
        blobs = listOf(
            Blob(0.2f, 0.2f, 0.7f, Color(0xFFFF7AD9)),
            Blob(0.85f, 0.8f, 0.8f, Color(0xFF7A3CFF))
        ),
        animated = false
    ),
    WallpaperPreset(
        id = "graphite",
        name = "Graphite",
        base = Color(0xFF0E0E10),
        blobs = listOf(
            Blob(0.8f, 0.1f, 0.8f, Color(0xFF4A4A58)),
            Blob(0.2f, 0.9f, 0.8f, Color(0xFF2C2C38))
        ),
        animated = false
    )
)

@Composable
fun PresetWallpaper(
    preset: WallpaperPreset,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    val phase: State<Float>? = if (animated && preset.animated) {
        val transition = rememberInfiniteTransition(label = "wallpaper")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
    } else {
        null
    }

    Canvas(modifier) {
        drawRect(preset.base)
        val span = max(size.width, size.height)
        val p = phase?.value ?: 0f
        preset.blobs.forEachIndexed { index, blob ->
            val dx = if (phase != null) sin(p + index * 2.1f) * 0.07f else 0f
            val dy = if (phase != null) cos(p + index * 1.7f) * 0.07f else 0f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset((blob.x + dx) * size.width, (blob.y + dy) * size.height),
                    radius = blob.radius * span
                )
            )
        }
    }
}

@Composable
fun WallpaperLayer(
    wallpaperId: String,
    photo: ImageBitmap?,
    dim: Int,
    modifier: Modifier = Modifier
) {
    val dimAlpha = when (dim) {
        0 -> 0f
        1 -> 0.28f
        else -> 0.5f
    }
    Box(modifier) {
        if (wallpaperId == "photo" && photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val preset = WallpaperPresets.firstOrNull { it.id == wallpaperId } ?: WallpaperPresets.first()
            PresetWallpaper(preset, Modifier.fillMaxSize(), animated = true)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.4f to Color.Transparent,
                    1f to Color(0xE60F0E15)
                )
            )
        )
    }
}
