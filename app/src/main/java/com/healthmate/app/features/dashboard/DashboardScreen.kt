package com.healthmate.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.constants.FoodStyle
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.ui.AppCard
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.ui.ErrorBanner
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.core.utils.formatted
import com.healthmate.app.features.records.data.DailyTotal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onRefresh = viewModel::load,
        onDateSelected = viewModel::selectDate,
        onMetricSelected = viewModel::selectChartMetric,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMetricSelected: (HealthMetric) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading && state.lastSevenDays.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(state) }

            if (state.lastSevenDays.isNotEmpty()) {
                item {
                    WeekStrip(
                        days = state.lastSevenDays,
                        selected = state.selectedDate,
                        today = state.today,
                        onSelect = onDateSelected,
                    )
                }
            }

            state.errorMessage?.let { message ->
                item { ErrorBanner(message = message, onRetry = onRefresh) }
            }

            item {
                Text(
                    if (state.isToday) "Today's activity" else "Activity on ${AppDates.shortDate(state.selectedDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            item { ActivityCard(state) }
            item { CalorieBalanceCard(state) }

            if (state.lastSevenDays.isNotEmpty()) {
                item {
                    WeeklyChart(
                        days = state.lastSevenDays,
                        metric = state.chartMetric,
                        goal = state.chartMetric.goalIn(state.goals),
                        average = state.weeklyAverage,
                        today = state.today,
                        onMetricSelected = onMetricSelected,
                    )
                }
            }
        }
    }
}

/** Avatar with initial, greeting and streak. */
@Composable
private fun Header(state: DashboardUiState) {
    val name = state.name.trim()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "H",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${greeting()}${if (name.isNotEmpty()) ", $name" else ""}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                AppDates.longDate(state.today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.streak > 0) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "🔥 ${state.streak}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics { contentDescription = "${state.streak}-day streak" },
                )
            }
        }
    }
}

/** Seven tappable day pills ending today. A dot shows days that have records. */
@Composable
private fun WeekStrip(
    days: List<DailyTotal>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            val isSelected = day.date == selected
            val colors = MaterialTheme.colorScheme
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) colors.primary else colors.surfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(onClickLabel = "Show ${AppDates.shortDate(day.date)}") { onSelect(day.date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val textColor = if (isSelected) colors.onPrimary else colors.onSurface
                Text(
                    if (day.date == today) "Today" else AppDates.weekday(day.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                )
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !day.hasData -> Color.Transparent
                                isSelected -> colors.onPrimary
                                else -> colors.primary
                            },
                        ),
                )
            }
        }
    }
}

/** Rings for steps, calories burned and water, with a legend of values and goals. */
@Composable
private fun ActivityCard(state: DashboardUiState) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                ActivityRings(
                    rings = HealthMetric.entries.map { Ring(state.progressFor(it), it.color) },
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 12.dp,
                    gap = 4.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.goalsReached}/3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("goals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HealthMetric.entries.forEach { metric ->
                    RingLegend(
                        metric = metric,
                        value = metric.valueIn(state.day),
                        goal = metric.goalIn(state.goals),
                        progress = state.progressFor(metric),
                    )
                }
            }
        }
    }
}

@Composable
private fun RingLegend(metric: HealthMetric, value: Int, goal: Int, progress: Float) {
    val percent = (progress * 100).roundToInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${metric.label}: ${value.formatted()} of ${goal.formatted()} ${metric.unit}, $percent percent"
        },
    ) {
        Icon(metric.icon, contentDescription = null, tint = metric.color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(metric.shortLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${value.formatted()} ${metric.unit}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (progress >= 1f) "Goal reached 🎉" else "$percent% of ${goal.formatted()}",
                style = MaterialTheme.typography.labelSmall,
                color = metric.color,
            )
        }
    }
}

/** Eaten vs burned, and progress toward the food calorie goal. */
@Composable
private fun CalorieBalanceCard(state: DashboardUiState) {
    val goal = state.goals.foodIntake
    val eaten = state.caloriesEaten
    val left = goal - eaten

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Calorie balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                BalanceItem("Eaten", eaten, FoodStyle.icon, FoodStyle.color, Modifier.weight(1f))
                VerticalDivider()
                BalanceItem("Burned", state.day.calories, HealthMetric.CALORIES.icon, HealthMetric.CALORIES.color, Modifier.weight(1f))
                VerticalDivider()
                BalanceItem(
                    label = "Net",
                    value = state.netCalories,
                    icon = null,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (eaten.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = FoodStyle.color,
                trackColor = FoodStyle.color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (left >= 0) "${eaten.formatted()} of ${goal.formatted()} kcal eaten · ${left.formatted()} left"
                else "${eaten.formatted()} kcal eaten · ${left.absoluteValue.formatted()} over your goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalanceItem(label: String, value: Int, icon: ImageVector?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            value.formatted(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
        )
        Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
