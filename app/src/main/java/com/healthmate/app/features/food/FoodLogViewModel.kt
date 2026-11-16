package com.healthmate.app.features.food

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.features.food.data.FoodEntry
import com.healthmate.app.features.food.data.FoodRepository
import com.healthmate.app.features.food.data.Meal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FoodLogUiState(
    val date: LocalDate,
    val isToday: Boolean = true,
    val entries: List<FoodEntry> = emptyList(),
    val goal: Int = 2000,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val total: Int get() = entries.sumOf { it.calories }
    val remaining: Int get() = goal - total
    val progress: Float get() = (total.toFloat() / goal).coerceIn(0f, 1f)

    fun entriesFor(meal: Meal): List<FoodEntry> = entries.filter { it.meal == meal }
}

/** Food diary for one day: entries grouped by meal, daily total and goal. */
class FoodLogViewModel(
    private val foodRepository: FoodRepository,
    settingsRepository: SettingsRepository,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodLogUiState(date = clock()))
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { foodRepository.changes.collect { load() } }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(goal = settings.goals.foodIntake) }
            }
        }
    }

    fun previousDay() = showDate(_uiState.value.date.minusDays(1))

    /** Moves forward one day, but never past today. */
    fun nextDay() {
        val next = _uiState.value.date.plusDays(1)
        if (!next.isAfter(clock())) showDate(next)
    }

    private fun showDate(date: LocalDate) {
        _uiState.update { it.copy(date = date, isToday = date == clock()) }
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = foodRepository.getEntries(_uiState.value.date)
                _uiState.update { it.copy(entries = entries, isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Loading food failed", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Could not load your food diary.") }
            }
        }
    }

    /** Removes the entry from the screen straight away, then from the database. */
    fun delete(entry: FoodEntry) {
        _uiState.update { state -> state.copy(entries = state.entries.filterNot { it.id == entry.id }) }
        viewModelScope.launch {
            try {
                foodRepository.deleteEntry(entry.id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Delete failed", e)
                _uiState.update { it.copy(errorMessage = "Could not delete the food.") }
                load()
            }
        }
    }

    fun undoDelete(entry: FoodEntry) {
        viewModelScope.launch {
            runCatching { foodRepository.restoreEntry(entry) }
                .onFailure { Log.e(TAG, "Undo failed", it) }
        }
    }

    private companion object {
        const val TAG = "FoodLogViewModel"
    }
}
