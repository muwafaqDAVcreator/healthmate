package com.healthmate.app.features.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One ring: how full it is (0..1) and its colour. */
data class Ring(val progress: Float, val color: Color)

/**
 * Concentric progress rings, outermost first. Each ring starts at the top
 * and fills clockwise; the faint track behind it shows the full goal.
 */
@Composable
fun ActivityRings(
    rings: List<Ring>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    gap: Dp = 5.dp,
) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(rings) {
        fill.snapTo(0f)
        fill.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val gapPx = gap.toPx()
        val diameter = size.minDimension
        val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

        rings.forEachIndexed { index, ring ->
            val inset = stroke / 2 + index * (stroke + gapPx)
            val arcSize = Size(diameter - inset * 2, diameter - inset * 2)
            if (arcSize.width <= 0f) return@forEachIndexed
            val topLeft = Offset(origin.x + inset, origin.y + inset)

            drawArc(
                color = ring.color.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = ring.color,
                startAngle = -90f,
                sweepAngle = 360f * ring.progress * fill.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
