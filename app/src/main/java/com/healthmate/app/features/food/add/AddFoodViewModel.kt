package com.healthmate.app.features.food.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.features.food.data.FoodCalories
import com.healthmate.app.features.food.data.FoodCatalog
import com.healthmate.app.features.food.data.FoodEntry
import com.healthmate.app.features.food.data.FoodItem
import com.healthmate.app.features.food.data.FoodRepository
import com.healthmate.app.features.food.data.Meal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddFoodUiState(
    val date: LocalDate,
    val query: String = "",
    val results: List<FoodItem> = FoodCatalog.items,
    /** The food picked from the list; null when nothing is picked or for a custom food. */
    val selectedItem: FoodItem? = null,
    /** True while the "custom food" sheet is open. */
    val isCustom: Boolean = false,
    val customName: String = "",
    val customKcal: String = "",
    val customNameError: String? = null,
    val customKcalError: String? = null,
    val servings: Double = 1.0,
    val meal: Meal = Meal.BREAKFAST,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Set after saving so the screen can close and show a message. */
    val savedMessage: String? = null,
) {
    val isSheetOpen: Boolean get() = selectedItem != null || isCustom

    val kcalPerServing: Int? get() = selectedItem?.kcalPerServing ?: customKcal.toIntOrNull()

    /** Live result of: calories per serving × servings. */
    val totalKcal: Int? get() = kcalPerServing?.let { FoodCalories.total(it, servings) }
}

/** Search the food list, choose servings and meal, and save to the food diary. */
class AddFoodViewModel(
    private val foodRepository: FoodRepository,
    savedStateHandle: SavedStateHandle,
    clock: () -> LocalDate = { LocalDate.now() },
    timeNow: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

    private val date: LocalDate = savedStateHandle.get<Long>(ARG_DATE)
        ?.takeIf { it != NO_DATE }
        ?.let(LocalDate::ofEpochDay)
        ?: clock()

    private val _uiState = MutableStateFlow(AddFoodUiState(date = date, meal = Meal.forTime(timeNow())))
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) =
        _uiState.update { it.copy(query = query, results = FoodCatalog.search(query)) }

    fun selectFood(item: FoodItem) =
        _uiState.update { it.copy(selectedItem = item, isCustom = false, servings = 1.0) }

    fun openCustomFood() = _uiState.update {
        it.copy(
            selectedItem = null,
            isCustom = true,
            customName = it.query.trim(),
            customKcal = "",
            customNameError = null,
            customKcalError = null,
            servings = 1.0,
        )
    }

    fun dismissSheet() = _uiState.update { it.copy(selectedItem = null, isCustom = false) }

    fun onMealChange(meal: Meal) = _uiState.update { it.copy(meal = meal) }

    fun increaseServings() = _uiState.update {
        it.copy(servings = (it.servings + FoodCalories.SERVING_STEP).coerceAtMost(FoodCalories.MAX_SERVINGS))
    }

    fun decreaseServings() = _uiState.update {
        it.copy(servings = (it.servings - FoodCalories.SERVING_STEP).coerceAtLeast(FoodCalories.MIN_SERVINGS))
    }

    fun onCustomNameChange(input: String) =
        _uiState.update { it.copy(customName = input.take(FoodCalories.MAX_NAME_LENGTH + 5), customNameError = null) }

    fun onCustomKcalChange(input: String) =
        _uiState.update { it.copy(customKcal = input.filter(Char::isDigit).take(4), customKcalError = null) }

    fun save() {
        val state = _uiState.value
        val name: String
        val kcalPerServing: Int
        if (state.isCustom) {
            val nameError = FoodCalories.validateName(state.customName)
            val kcalError = FoodCalories.validateKcal(state.customKcal)
            if (nameError != null || kcalError != null) {
                _uiState.update { it.copy(customNameError = nameError, customKcalError = kcalError) }
                return
            }
            name = state.customName.trim()
            kcalPerServing = state.customKcal.toInt()
        } else {
            val item = state.selectedItem ?: return
            name = item.name
            kcalPerServing = item.kcalPerServing
        }

        val entry = FoodEntry(
            date = date,
            meal = state.meal,
            name = name,
            servings = state.servings,
            calories = FoodCalories.total(kcalPerServing, state.servings),
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                foodRepository.addEntry(entry)
                _uiState.update {
                    it.copy(isSaving = false, savedMessage = "Added $name (${entry.calories} kcal)")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Saving food failed", e)
                _uiState.update { it.copy(isSaving = false, errorMessage = "Could not add the food. Please try again.") }
            }
        }
    }

    companion object {
        const val ARG_DATE = "date"
        const val NO_DATE = Long.MIN_VALUE
        private const val TAG = "AddFoodViewModel"
    }
}
