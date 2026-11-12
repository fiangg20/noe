package com.noe.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.noe.app.data.FormatState
import com.noe.app.data.SpanKind
import com.noe.app.data.TextSizes
import com.noe.app.data.TextSpan
import java.util.TreeSet

/**
 * Turns plain text plus formatting spans into styled text. The text is cut at every point
 * where some span starts or ends, so overlapping styles (for example underline and
 * strikethrough) are combined instead of one replacing the other.
 */
fun styledText(text: String, spans: List<TextSpan>): AnnotatedString {
    if (spans.isEmpty() || text.isEmpty()) return AnnotatedString(text)
    val len = text.length

    val points = TreeSet<Int>()
    points.add(0)
    points.add(len)
    spans.forEach {
        points.add(it.start.coerceIn(0, len))
        points.add(it.end.coerceIn(0, len))
    }
    val cuts = points.toList()

    return buildAnnotatedString {
        append(text)
        for (i in 0 until cuts.size - 1) {
            val a = cuts[i]
            val b = cuts[i + 1]
            if (a >= b) continue
            val active = spans.filter { it.start <= a && it.end >= b }
            if (active.isEmpty()) continue

            val decorations = ArrayList<TextDecoration>()
            if (active.any { it.kind == SpanKind.Underline }) decorations.add(TextDecoration.Underline)
            if (active.any { it.kind == SpanKind.Strike }) decorations.add(TextDecoration.LineThrough)
            val scale = active.firstOrNull { it.kind == SpanKind.Size }?.value ?: 1f

            addStyle(
                SpanStyle(
                    fontWeight = if (active.any { it.kind == SpanKind.Bold }) FontWeight.Bold else null,
                    fontStyle = if (active.any { it.kind == SpanKind.Italic }) FontStyle.Italic else null,
                    textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations),
                    fontSize = if (scale == 1f) TextUnit.Unspecified else scale.em,
                ),
                a,
                b,
            )
        }
    }
}

/** Shows the formatting inside an editable text field without changing the text itself. */
class SpanTransformation(private val spans: List<TextSpan>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(styledText(text.text, spans), OffsetMapping.Identity)
}

@Composable
private fun FormatButton(
    active: Boolean,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val corner by animateDpAsState(
        targetValue = if (active) 20.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "formatCorner",
    )
    val fill by animateColorAsState(
        targetValue = if (active) tint.copy(alpha = 0.2f) else Color.Transparent,
        label = "formatFill",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = active
            },
        contentAlignment = Alignment.Center,
    ) {
        label()
    }
}

/** Scrollable row of formatting buttons shown above the editor's bottom bar. */
@Composable
fun FormatBar(
    format: FormatState,
    tint: Color,
    onToggle: (SpanKind) -> Unit,
    onSize: (Float) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatButton(format.bold, "Bold", tint, { onToggle(SpanKind.Bold) }) {
            Text("B", color = tint, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        }
        FormatButton(format.italic, "Italic", tint, { onToggle(SpanKind.Italic) }) {
            Text("I", color = tint, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.titleMedium)
        }
        FormatButton(format.underline, "Underline", tint, { onToggle(SpanKind.Underline) }) {
            Text(
                "U",
                color = tint,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        FormatButton(format.strike, "Strikethrough", tint, { onToggle(SpanKind.Strike) }) {
            Text(
                "S",
                color = tint,
                textDecoration = TextDecoration.LineThrough,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.width(8.dp))

        val sizes = listOf(
            Triple(TextSizes.Small, "Small text", 12.sp),
            Triple(TextSizes.Normal, "Normal text", 16.sp),
            Triple(TextSizes.Large, "Large text", 20.sp),
            Triple(TextSizes.Huge, "Huge text", 24.sp),
        )
        sizes.forEach { (scale, description, fontSize) ->
            FormatButton(format.size == scale, description, tint, { onSize(scale) }) {
                Text("A", color = tint, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.width(8.dp))

        FormatButton(false, "Clear formatting", tint, onClear) {
            Icon(Icons.Outlined.FormatClear, contentDescription = null, tint = tint)
        }
    }
}
