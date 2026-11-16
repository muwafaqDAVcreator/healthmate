package com.healthmate.app.features.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.ui.AppCard
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.core.utils.compact
import com.healthmate.app.core.utils.formatted
import com.healthmate.app.features.records.data.DailyTotal
import java.time.LocalDate
import kotlin.math.roundToInt

/** Card with a metric picker and a bar chart of the last 7 days. */
@Composable
fun WeeklyChart(
    days: List<DailyTotal>,
    metric: HealthMetric,
    goal: Int,
    average: Double,
    today: LocalDate,
    onMetricSelected: (HealthMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HealthMetric.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = item == metric,
                        onClick = { onMetricSelected(item) },
                        shape = SegmentedButtonDefaults.itemShape(index, HealthMetric.entries.size),
                    ) {
                        Text(item.shortLabel)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            BarChart(
                days = days,
                metric = metric,
                goal = goal,
                today = today,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )

            // Day names under the bars.
            Row(Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    Text(
                        text = if (day.date == today) "Today" else AppDates.weekday(day.date),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Daily average: ${average.roundToInt().formatted()} ${metric.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Hand-drawn bar chart. Today's bar is solid, other days are lighter, and a
 * dashed line shows the daily goal. Bars grow in when the data or metric changes.
 */
@Composable
private fun BarChart(
    days: List<DailyTotal>,
    metric: HealthMetric,
    goal: Int,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val goalColor = MaterialTheme.colorScheme.outline
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val growth = remember(days, metric, goal) { Animatable(0f) }
    LaunchedEffect(days, metric, goal) { growth.animateTo(1f, tween(durationMillis = 600)) }

    val values = days.map { metric.valueIn(it) }
    val description = days.zip(values).joinToString { (day, value) ->
        "${AppDates.weekday(day.date)} ${value.formatted()}"
    }

    Canvas(
        modifier.semantics { contentDescription = "${metric.label} for the last 7 days: $description" },
    ) {
        if (days.isEmpty()) return@Canvas

        val topSpace = 18.dp.toPx() // room for the value labels
        val chartHeight = size.height - topSpace
        val maxValue = maxOf(values.maxOrNull() ?: 0, goal) * 1.1f
        val slot = size.width / days.size
        val barWidth = slot * 0.55f

        // Goal line
        val goalY = topSpace + chartHeight * (1 - goal / maxValue)
        drawLine(
            color = goalColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "Goal",
            style = labelStyle,
            topLeft = Offset(size.width - 26.dp.toPx(), goalY - 14.dp.toPx()),
        )

        // Bars
        days.forEachIndexed { index, day ->
            val value = values[index]
            val barHeight = chartHeight * (value / maxValue) * growth.value
            val left = slot * index + (slot - barWidth) / 2
            val top = size.height - barHeight
            drawRoundRect(
                color = metric.color.copy(alpha = if (day.date == today) 1f else 0.55f),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            if (value > 0) {
                val label = textMeasurer.measure(value.compact(), labelStyle)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        x = left + (barWidth - label.size.width) / 2,
                        y = top - label.size.height - 2.dp.toPx(),
                    ),
                )
            }
        }
    }
}
