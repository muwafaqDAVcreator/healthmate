package com.healthmate.app.features.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.features.food.data.FoodRepository
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
    /** Today's date (the week strip always ends here). */
    val today: LocalDate = LocalDate.now(),
    /** The day picked in the week strip; today by default. */
    val selectedDate: LocalDate = today,
    /** Steps, calories burned and water for [selectedDate]. */
    val day: DailyTotal = DailyTotal(selectedDate),
    /** Calories eaten on [selectedDate] from the food diary. */
    val caloriesEaten: Int = 0,
    val lastSevenDays: List<DailyTotal> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val chartMetric: HealthMetric = HealthMetric.STEPS,
    val name: String = "",
    val goals: Goals = Goals(),
) {
    val isToday: Boolean get() = selectedDate == today

    /** Average of the chart's metric over the last 7 days. */
    val weeklyAverage: Double
        get() = if (lastSevenDays.isEmpty()) 0.0
        else lastSevenDays.sumOf { chartMetric.valueIn(it) }.toDouble() / lastSevenDays.size

    /** How many days in a row (ending today) have at least one record. */
    val streak: Int
        get() = lastSevenDays.asReversed().takeWhile { it.hasData }.size

    /** How many of the three activity goals are met on the selected day. */
    val goalsReached: Int
        get() = HealthMetric.entries.count { progressFor(it) >= 1f }

    /** Calories eaten minus calories burned. */
    val netCalories: Int get() = caloriesEaten - day.calories

    /** Fraction of the goal reached, capped at 1 for rings and bars. */
    fun progressFor(metric: HealthMetric): Float =
        (metric.valueIn(day).toFloat() / metric.goalIn(goals)).coerceIn(0f, 1f)
}

class DashboardViewModel(
    private val repository: HealthRecordRepository,
    private val foodRepository: FoodRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(today = clock()))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Reload whenever a record or food entry is added, edited or deleted.
        viewModelScope.launch { repository.changes.collect { load() } }
        viewModelScope.launch { foodRepository.changes.collect { load() } }
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

    /** Show the totals for another day from the week strip. */
    fun selectDate(date: LocalDate) {
        if (date.isAfter(clock())) return
        _uiState.update { it.copy(selectedDate = date) }
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val today = clock()
            _uiState.update { it.copy(isLoading = true, errorMessage = null, today = today) }
            try {
                val start = today.minusDays(6)
                val totals = repository.getDailyTotals(start, today).associateBy { it.date }

                // The query skips days without records, so fill those with zeros
                // to always have exactly 7 days in the strip and chart.
                val week = (0..6L).map { offset ->
                    val date = start.plusDays(offset)
                    totals[date] ?: DailyTotal(date)
                }
                val selected = _uiState.value.selectedDate

                _uiState.update {
                    it.copy(
                        day = repository.getTotalsForDate(selected),
                        caloriesEaten = foodRepository.getTotalCalories(selected),
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
