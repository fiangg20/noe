package com.noe.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A checkbox that morphs from a rounded square into a circle with a bouncy spring,
 * the way Material 3 Expressive toggles feel.
 */
@Composable
fun MorphCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color,
    checkColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    touch: Dp = 40.dp,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    val corner by animateDpAsState(
        targetValue = if (checked) size / 2 else size / 4,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "morphCorner",
    )
    val fill by animateColorAsState(
        targetValue = if (checked) tint else Color.Transparent,
        label = "morphFill",
    )
    val tick by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = bouncy,
        label = "morphTick",
    )

    val shape = RoundedCornerShape(corner.coerceIn(0.dp, size / 2))

    Box(
        modifier = modifier
            .size(maxOf(touch, size))
            .clip(CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(fill)
                .border(2.dp, tint, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier
                    .size(size * 0.7f)
                    .graphicsLayer {
                        scaleX = tick
                        scaleY = tick
                        alpha = tick.coerceIn(0f, 1f)
                    },
            )
        }
    }
}

/** A progress bar drawn as a wave. Pass animate = true to make the wave travel. */
@Composable
fun WavyProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 20.dp,
) {
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wavyProgress",
    )
    val transition = rememberInfiniteTransition(label = "wavyPhase")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "wavyPhaseValue",
    )

    Canvas(modifier = modifier.height(amplitude * 2 + strokeWidth)) {
        val sw = strokeWidth.toPx()
        val amp = amplitude.toPx()
        val wl = wavelength.toPx()
        val inset = sw / 2f
        val cy = size.height / 2f
        val endX = inset + (size.width - 2f * inset) * shown
        val phase = if (animate) phaseState.value else 0f

        // Faded track for the part that is not done yet.
        drawLine(
            color = color.copy(alpha = 0.25f),
            start = Offset(endX, cy),
            end = Offset(size.width - inset, cy),
            strokeWidth = sw,
            cap = StrokeCap.Round,
        )

        if (shown > 0.001f) {
            val path = Path()
            var x = inset
            var first = true
            while (x <= endX) {
                val y = cy + amp * sin((x / wl) * 2f * PI.toFloat() - phase)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 2f
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** A letter on a slowly rotating cookie shape, used as the profile badge on the home screen. */
@Composable
fun CookieAvatar(
    letter: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    container: Color = MaterialTheme.colorScheme.tertiaryContainer,
    content: Color = MaterialTheme.colorScheme.onTertiaryContainer,
) {
    val shape = remember { CookieShape(8, 0.08f) }
    val transition = rememberInfiniteTransition(label = "avatarSpin")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "avatarAngle",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = angle.value }
                .clip(shape)
                .background(container),
        )
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            color = content,
        )
    }
}
