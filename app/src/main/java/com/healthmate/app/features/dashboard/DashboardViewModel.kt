package com.healthmate.app.features.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.features.records.data.DailyTotal
import com.healthmate.app.features.records.data.HealthRecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the dashboard screen draws. */
data class DashboardUiState(
    val today: DailyTotal = DailyTotal(LocalDate.now()),
    val lastSevenDays: List<DailyTotal> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val chartMetric: HealthMetric = HealthMetric.STEPS,
    val name: String = "",
    val goals: Goals = Goals(),
) {
    /** Average of the chart's metric over the last 7 days. */
    val weeklyAverage: Double
        get() = if (lastSevenDays.isEmpty()) 0.0
        else lastSevenDays.sumOf { chartMetric.valueIn(it) }.toDouble() / lastSevenDays.size

    /** How many days in a row (ending today) have at least one record. */
    val streak: Int
        get() = lastSevenDays.asReversed().takeWhile { it.hasData }.size

    /** How many of the three daily goals are already met today. */
    val goalsReached: Int
        get() = HealthMetric.entries.count { progressFor(it) >= 1f }

    /** Fraction of today's goal reached, capped at 1 for the progress bar. */
    fun progressFor(metric: HealthMetric): Float =
        (metric.valueIn(today).toFloat() / metric.goalIn(goals)).coerceIn(0f, 1f)
}

class DashboardViewModel(
    private val repository: HealthRecordRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(today = DailyTotal(clock())))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Reload whenever a record is added, edited or deleted anywhere in the app.
        viewModelScope.launch {
            repository.changes.collect { load() }
        }
        // Use the latest name and goals from the Settings screen.
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(name = settings.name, goals = settings.goals) }
            }
        }
    }

    fun selectChartMetric(metric: HealthMetric) {
        _uiState.update { it.copy(chartMetric = metric) }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val today = clock()
                val start = today.minusDays(6)
                val totals = repository.getDailyTotals(start, today).associateBy { it.date }

                // The query skips days without records, so fill those with zeros
                // to always have exactly 7 bars in the chart.
                val week = (0..6L).map { offset ->
                    val date = start.plusDays(offset)
                    totals[date] ?: DailyTotal(date)
                }

                _uiState.update {
                    it.copy(
                        today = repository.getTotalsForDate(today),
                        lastSevenDays = week,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Dashboard load failed", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Could not load your summary. Please try again.")
                }
            }
        }
    }

    private companion object {
        const val TAG = "DashboardViewModel"
    }
}
