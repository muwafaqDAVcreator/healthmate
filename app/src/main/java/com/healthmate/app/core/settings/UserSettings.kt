package com.healthmate.app.core.settings

import com.healthmate.app.core.constants.DailyGoals

/** The user's own daily targets. Defaults come from [DailyGoals]. */
data class Goals(
    val steps: Int = DailyGoals.STEPS,
    val calories: Int = DailyGoals.CALORIES,
    val water: Int = DailyGoals.WATER,
    /** Calories to eat per day, for the food diary. */
    val foodIntake: Int = DailyGoals.FOOD_INTAKE,
)

/**
 * Profile and goals from the Settings screen.
 * Height and weight are optional; they are only needed to calculate calories.
 */
data class UserSettings(
    val name: String = "",
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val goals: Goals = Goals(),
) {
    val hasBodyMeasurements: Boolean get() = heightCm != null && weightKg != null

    /** Body Mass Index = weight (kg) / height (m)², or null if not set. */
    val bmi: Double?
        get() = bmiOf(heightCm, weightKg)

    companion object {
        fun bmiOf(heightCm: Int?, weightKg: Int?): Double? {
            if (heightCm == null || weightKg == null || heightCm <= 0) return null
            val heightM = heightCm / 100.0
            return weightKg / (heightM * heightM)
        }

        /** NHS / WHO adult ranges. */
        fun bmiCategory(bmi: Double): String = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Healthy weight"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }
}
