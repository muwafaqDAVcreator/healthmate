package com.healthmate.app.core.utils

import com.healthmate.app.core.constants.InputLimits
import java.time.LocalDate

/**
 * Validation rules for the Add / Edit record form.
 * Each function returns an error message, or null when the value is fine.
 */
object RecordValidators {

    fun steps(input: String): String? =
        wholeNumber(input, label = "steps", max = InputLimits.MAX_STEPS)

    fun calories(input: String): String? =
        wholeNumber(input, label = "calories", max = InputLimits.MAX_CALORIES)

    fun water(input: String): String? =
        wholeNumber(input, label = "water intake", max = InputLimits.MAX_WATER)

    fun date(date: LocalDate?, today: LocalDate = LocalDate.now()): String? = when {
        date == null -> "Please choose a date"
        date.isAfter(today) -> "Date cannot be in the future"
        else -> null
    }

    /** A record where everything is 0 is almost certainly a mistake. */
    fun atLeastOneValue(steps: Int, calories: Int, water: Int): String? =
        if (steps == 0 && calories == 0 && water == 0) "Enter at least one value above 0" else null

    private fun wholeNumber(input: String, label: String, max: Int): String? {
        val text = input.trim()
        if (text.isEmpty()) return "Please enter $label (use 0 if none)"

        val number = text.toIntOrNull() ?: return "Use whole numbers only"
        if (number < 0) return "Cannot be negative"
        if (number > max) return "That looks too high (max ${max.formatted()})"
        return null
    }
}
