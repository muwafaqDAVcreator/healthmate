package com.healthmate.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.ui.ErrorBanner
import com.healthmate.app.core.utils.AppDates
import java.time.LocalTime

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onRefresh = viewModel::load,
        onMetricSelected = viewModel::selectChartMetric,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GreetingCard(state) }

            state.errorMessage?.let { message ->
                item { ErrorBanner(message = message, onRetry = onRefresh) }
            }

            item {
                Text(
                    "Today's progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }

            items(HealthMetric.entries) { metric ->
                SummaryCard(
                    metric = metric,
                    value = metric.valueIn(state.today),
                    goal = metric.goalIn(state.goals),
                    progress = state.progressFor(metric),
                )
            }

            if (state.lastSevenDays.isNotEmpty()) {
                item {
                    WeeklyChart(
                        days = state.lastSevenDays,
                        metric = state.chartMetric,
                        goal = state.chartMetric.goalIn(state.goals),
                        average = state.weeklyAverage,
                        today = state.today.date,
                        onMetricSelected = onMetricSelected,
                    )
                }
            }
        }
    }
}

/** Teal gradient card at the top: greeting with the user's name, date, streak and goals met. */
@Composable
private fun GreetingCard(state: DashboardUiState) {
    val colors = MaterialTheme.colorScheme
    val greeting = greeting() + if (state.name.isNotBlank()) ", ${state.name.trim()}" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(colors.primary, Color(0xFF26A69A))))
            .padding(20.dp),
    ) {
        Text("HealthMate", style = MaterialTheme.typography.labelLarge, color = colors.onPrimary.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Text(
            greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
        )
        Text(
            AppDates.longDate(state.today.date),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimary.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.streak > 0) Pill("🔥 ${state.streak}-day streak")
            Pill("🎯 ${state.goalsReached} of 3 goals met")
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
