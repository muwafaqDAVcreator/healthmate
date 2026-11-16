package com.healthmate.app.core.constants

/**
 * Daily targets shown on the dashboard progress bars.
 * These follow common guidance (about 10,000 steps and 2.5 litres of water).
 */
object DailyGoals {
    const val STEPS = 10_000
    const val CALORIES = 500 // kcal burned through activity
    const val WATER = 2_500 // ml
}

/**
 * Upper limits used by the form so obvious typos (e.g. 700000 steps) are
 * caught before they reach the database.
 */
object InputLimits {
    const val MAX_STEPS = 100_000
    const val MAX_CALORIES = 10_000
    const val MAX_WATER = 10_000

    /** Longest number the form fields accept. */
    const val MAX_DIGITS = 6
}
