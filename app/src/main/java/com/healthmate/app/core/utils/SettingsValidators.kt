package com.healthmate.app.core.utils

/**
 * Validation rules for the Settings screen.
 * Each function returns an error message, or null when the value is fine.
 */
object SettingsValidators {
    const val MAX_NAME_LENGTH = 30

    fun name(input: String): String? =
        if (input.trim().length > MAX_NAME_LENGTH) "Name is too long (max $MAX_NAME_LENGTH characters)" else null

    /** Height is optional, but if given it must be realistic. */
    fun height(input: String): String? = optionalRange(input, 100, 250, "Height", "cm")

    /** Weight is optional, but if given it must be realistic. */
    fun weight(input: String): String? = optionalRange(input, 30, 300, "Weight", "kg")

    fun stepsGoal(input: String): String? = requiredRange(input, 1_000, 100_000, "Steps goal")
    fun caloriesGoal(input: String): String? = requiredRange(input, 50, 5_000, "Calories goal")
    fun waterGoal(input: String): String? = requiredRange(input, 500, 10_000, "Water goal")

    private fun optionalRange(input: String, min: Int, max: Int, label: String, unit: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val value = text.toIntOrNull() ?: return "Use whole numbers only"
        return if (value !in min..max) "$label should be between $min and $max $unit" else null
    }

    private fun requiredRange(input: String, min: Int, max: Int, label: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return "Please enter a goal"
        val value = text.toIntOrNull() ?: return "Use whole numbers only"
        return if (value !in min..max) "$label should be between ${min.formatted()} and ${max.formatted()}" else null
    }
}
