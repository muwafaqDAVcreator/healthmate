package com.healthmate.app.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.constants.InputLimits
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.core.settings.UserSettings
import com.healthmate.app.core.utils.SettingsValidators
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val name: String = "",
    val height: String = "",
    val weight: String = "",
    val stepsGoal: String = "",
    val caloriesGoal: String = "",
    val waterGoal: String = "",
    val foodGoal: String = "",
    val nameError: String? = null,
    val heightError: String? = null,
    val weightError: String? = null,
    val stepsGoalError: String? = null,
    val caloriesGoalError: String? = null,
    val waterGoalError: String? = null,
    val foodGoalError: String? = null,
    val isSaving: Boolean = false,
    /** Short message for a snackbar, cleared once it has been shown. */
    val message: String? = null,
) {
    /** BMI from what is typed right now, so it updates while the user types. */
    val bmi: Double?
        get() = if (heightError == null && weightError == null) {
            UserSettings.bmiOf(height.toIntOrNull(), weight.toIntOrNull())
        } else {
            null
        }

    val heightCm: Int? get() = height.toIntOrNull()
    val weightKg: Int? get() = weight.toIntOrNull()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(fromSettings(settingsRepository.settings.value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onNameChange(input: String) =
        edit { it.copy(name = input.take(SettingsValidators.MAX_NAME_LENGTH + 5), nameError = null) }

    fun onHeightChange(input: String) = edit { it.copy(height = digits(input, 3), heightError = null) }
    fun onWeightChange(input: String) = edit { it.copy(weight = digits(input, 3), weightError = null) }
    fun onStepsGoalChange(input: String) = edit { it.copy(stepsGoal = digits(input), stepsGoalError = null) }
    fun onCaloriesGoalChange(input: String) = edit { it.copy(caloriesGoal = digits(input), caloriesGoalError = null) }
    fun onWaterGoalChange(input: String) = edit { it.copy(waterGoal = digits(input), waterGoalError = null) }
    fun onFoodGoalChange(input: String) = edit { it.copy(foodGoal = digits(input), foodGoalError = null) }

    fun save() {
        val checked = _uiState.value.let {
            it.copy(
                nameError = SettingsValidators.name(it.name),
                heightError = SettingsValidators.height(it.height),
                weightError = SettingsValidators.weight(it.weight),
                stepsGoalError = SettingsValidators.stepsGoal(it.stepsGoal),
                caloriesGoalError = SettingsValidators.caloriesGoal(it.caloriesGoal),
                waterGoalError = SettingsValidators.waterGoal(it.waterGoal),
                foodGoalError = SettingsValidators.foodGoal(it.foodGoal),
            )
        }
        _uiState.value = checked
        val hasErrors = listOf(
            checked.nameError, checked.heightError, checked.weightError,
            checked.stepsGoalError, checked.caloriesGoalError, checked.waterGoalError, checked.foodGoalError,
        ).any { it != null }
        if (hasErrors) return

        val settings = UserSettings(
            name = checked.name.trim(),
            heightCm = checked.heightCm,
            weightKg = checked.weightKg,
            goals = Goals(
                steps = checked.stepsGoal.toInt(),
                calories = checked.caloriesGoal.toInt(),
                water = checked.waterGoal.toInt(),
                foodIntake = checked.foodGoal.toInt(),
            ),
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                settingsRepository.save(settings)
                _uiState.update { it.copy(isSaving = false, message = "Settings saved") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Saving settings failed", e)
                _uiState.update { it.copy(isSaving = false, message = "Could not save settings") }
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private inline fun edit(change: (SettingsUiState) -> SettingsUiState) = _uiState.update(change)

    private fun digits(input: String, maxLength: Int = InputLimits.MAX_DIGITS) =
        input.filter(Char::isDigit).take(maxLength)

    private fun fromSettings(settings: UserSettings) = SettingsUiState(
        name = settings.name,
        height = settings.heightCm?.toString() ?: "",
        weight = settings.weightKg?.toString() ?: "",
        stepsGoal = settings.goals.steps.toString(),
        caloriesGoal = settings.goals.calories.toString(),
        waterGoal = settings.goals.water.toString(),
        foodGoal = settings.goals.foodIntake.toString(),
    )

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
