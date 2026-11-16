package com.healthmate.app.features.records.form

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.constants.InputLimits
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.core.utils.CalorieCalculator
import com.healthmate.app.core.utils.RecordValidators
import com.healthmate.app.features.records.data.HealthRecord
import com.healthmate.app.features.records.data.HealthRecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/** What happened when the form closed, so the app can show the right message. */
enum class FormResult(val message: String) {
    SAVED("Record saved"),
    UPDATED("Record updated"),
    DELETED("Record deleted"),
}

data class RecordFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val steps: String = "",
    val calories: String = "",
    val water: String = "",
    val dateError: String? = null,
    val stepsError: String? = null,
    val caloriesError: String? = null,
    val waterError: String? = null,
    /** Error for the whole form, e.g. every value is 0 or saving failed. */
    val formError: String? = null,
    val isSaving: Boolean = false,
    val result: FormResult? = null,
    /** True when height and weight are set in Settings, so calories can be calculated. */
    val canCalculateCalories: Boolean = false,
    /** Explains the last calculated calorie value, e.g. "≈ 5.7 km walked at 70 kg". */
    val calorieHint: String? = null,
)

/**
 * Add and Edit share this view model. When the screen is opened with a
 * record id it loads that record and switches to edit mode.
 */
class RecordFormViewModel(
    private val repository: HealthRecordRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val recordId: Long = savedStateHandle.get<Long>(ARG_RECORD_ID) ?: NO_ID

    private val _uiState = MutableStateFlow(
        RecordFormUiState(
            isEditMode = recordId != NO_ID,
            isLoading = recordId != NO_ID,
            date = clock(),
            canCalculateCalories = settingsRepository.settings.value.hasBodyMeasurements,
        ),
    )
    val uiState: StateFlow<RecordFormUiState> = _uiState.asStateFlow()

    /** Errors are only shown after the first Save tap, then update as the user types. */
    private var showErrors = false

    init {
        if (recordId != NO_ID) loadRecord()
    }

    private fun loadRecord() {
        viewModelScope.launch {
            val record = runCatching { repository.getRecordById(recordId) }.getOrNull()
            _uiState.update {
                if (record == null) {
                    it.copy(isLoading = false, formError = "This record could not be found.")
                } else {
                    it.copy(
                        isLoading = false,
                        date = record.date,
                        steps = record.steps.toString(),
                        calories = record.calories.toString(),
                        water = record.water.toString(),
                    )
                }
            }
        }
    }

    fun onDateChange(date: LocalDate) = edit { it.copy(date = date) }

    // Only digits are kept, so letters, signs and decimals can't be typed.
    fun onStepsChange(input: String) = edit { it.copy(steps = digitsOnly(input), calorieHint = null) }
    fun onCaloriesChange(input: String) = edit { it.copy(calories = digitsOnly(input), calorieHint = null) }
    fun onWaterChange(input: String) = edit { it.copy(water = digitsOnly(input)) }

    /**
     * Fills the calories field from the steps typed in, using the height and
     * weight saved in Settings (see [CalorieCalculator] for the formula).
     */
    fun calculateCalories() {
        val settings = settingsRepository.settings.value
        val height = settings.heightCm
        val weight = settings.weightKg
        if (height == null || weight == null) {
            _uiState.update { it.copy(formError = "Add your height and weight in Settings first") }
            return
        }
        val steps = _uiState.value.steps.toIntOrNull()
        if (steps == null || steps == 0) {
            _uiState.update { it.copy(stepsError = "Enter your steps first to calculate calories") }
            return
        }
        val kcal = CalorieCalculator.caloriesFromSteps(steps, height, weight)
        val km = CalorieCalculator.distanceKm(steps, height)
        edit {
            it.copy(
                calories = kcal.toString(),
                calorieHint = "Estimated: ${String.format(Locale.UK, "%.1f", km)} km walked at $weight kg",
            )
        }
    }

    fun save() {
        showErrors = true
        val state = validated(_uiState.value)
        _uiState.value = state
        if (state.hasFieldErrors()) return

        val steps = state.steps.toInt()
        val calories = state.calories.toInt()
        val water = state.water.toInt()
        RecordValidators.atLeastOneValue(steps, calories, water)?.let { message ->
            _uiState.update { it.copy(formError = message) }
            return
        }

        val record = HealthRecord(
            id = if (state.isEditMode) recordId else 0,
            date = state.date,
            steps = steps,
            calories = calories,
            water = water,
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (state.isEditMode) {
                    repository.updateRecord(record)
                    _uiState.update { it.copy(isSaving = false, result = FormResult.UPDATED) }
                } else {
                    repository.addRecord(record)
                    _uiState.update { it.copy(isSaving = false, result = FormResult.SAVED) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Saving failed", e)
                _uiState.update { it.copy(isSaving = false, formError = "Could not save. Please try again.") }
            }
        }
    }

    fun delete() {
        if (recordId == NO_ID) return
        viewModelScope.launch {
            try {
                repository.deleteRecord(recordId)
                _uiState.update { it.copy(result = FormResult.DELETED) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Delete failed", e)
                _uiState.update { it.copy(formError = "Could not delete. Please try again.") }
            }
        }
    }

    private inline fun edit(change: (RecordFormUiState) -> RecordFormUiState) {
        _uiState.update { current ->
            val changed = change(current).copy(formError = null)
            if (showErrors) validated(changed) else changed
        }
    }

    private fun validated(state: RecordFormUiState) = state.copy(
        dateError = RecordValidators.date(state.date, clock()),
        stepsError = RecordValidators.steps(state.steps),
        caloriesError = RecordValidators.calories(state.calories),
        waterError = RecordValidators.water(state.water),
    )

    private fun RecordFormUiState.hasFieldErrors() =
        listOf(dateError, stepsError, caloriesError, waterError).any { it != null }

    private fun digitsOnly(input: String) = input.filter(Char::isDigit).take(InputLimits.MAX_DIGITS)

    companion object {
        const val ARG_RECORD_ID = "recordId"
        const val NO_ID = -1L
        private const val TAG = "RecordFormViewModel"
    }
}
