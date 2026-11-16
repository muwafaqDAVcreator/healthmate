package com.healthmate.app.features.food.data

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Food calorie maths and validation.
 *
 * Calories eaten = calories per serving × number of servings
 * e.g. 1.5 cups of white rice = 1.5 × 240 kcal = 360 kcal.
 */
object FoodCalories {
    const val MIN_SERVINGS = 0.5
    const val MAX_SERVINGS = 10.0
    const val SERVING_STEP = 0.5
    const val MAX_NAME_LENGTH = 40
    const val MAX_KCAL_PER_SERVING = 3000

    fun total(kcalPerServing: Int, servings: Double): Int = (kcalPerServing * servings).roundToInt()

    /** 1.0 -> "1", 1.5 -> "1.5" */
    fun formatServings(servings: Double): String =
        if (servings % 1.0 == 0.0) servings.toInt().toString() else String.format(Locale.UK, "%.1f", servings)

    fun validateName(input: String): String? = when {
        input.isBlank() -> "Please enter the food name"
        input.trim().length > MAX_NAME_LENGTH -> "Name is too long (max $MAX_NAME_LENGTH characters)"
        else -> null
    }

    fun validateKcal(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return "Please enter calories per serving"
        val value = text.toIntOrNull() ?: return "Use whole numbers only"
        return when {
            value <= 0 -> "Calories must be above 0"
            value > MAX_KCAL_PER_SERVING -> "That looks too high (max 3,000)"
            else -> null
        }
    }
}
