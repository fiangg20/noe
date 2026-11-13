@file:OptIn(ExperimentalFoundationApi::class)

package dev.expressive.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun FocusSurface(
    modifier: Modifier = Modifier,
    shapeAt: ShapeAt,
    containerColor: Color,
    ringColor: Color,
    focusedScale: Float = 1.08f,
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (focused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    val morph = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "morph"
    )

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
                shape = shapeAt(morph.value, this)
                clip = true
                val m = morph.value.coerceIn(0f, 1f)
                shadowElevation = 28f * m
                spotShadowColor = ringColor
                ambientShadowColor = ringColor
            }
            .background(containerColor)
            .drawWithContent {
                drawContent()
                val m = morph.value.coerceIn(0f, 1f)
                if (m > 0.02f) {
                    val outline = shapeAt(m, this).createOutline(size, layoutDirection, this)
                    drawPath(
                        path = outline.toPath(),
                        color = ringColor.copy(alpha = m),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            }
            .onFocusChanged { state ->
                focused = state.isFocused
                onFocusChange(state.isFocused)
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        content = content
    )
}
