package com.healthmate.app.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HealthMate", fontWeight = FontWeight.Bold) })
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.lastSevenDays.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Header(state) }

                state.errorMessage?.let { message ->
                    item { ErrorBanner(message = message, onRetry = onRefresh) }
                }

                items(HealthMetric.entries) { metric ->
                    SummaryCard(
                        metric = metric,
                        value = metric.valueIn(state.today),
                        progress = state.progressFor(metric),
                    )
                }

                if (state.lastSevenDays.isNotEmpty()) {
                    item {
                        WeeklyChart(
                            days = state.lastSevenDays,
                            metric = state.chartMetric,
                            average = state.weeklyAverage,
                            today = state.today.date,
                            onMetricSelected = onMetricSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "Today · ${AppDates.longDate(state.today.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.streak > 0) {
            AssistChip(
                onClick = {},
                label = { Text("🔥 ${state.streak}-day streak") },
            )
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
