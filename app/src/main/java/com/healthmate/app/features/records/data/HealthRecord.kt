package com.healthmate.app.features.records.data

import java.time.LocalDate

/**
 * One row of the `health_records` table.
 * [id] is 0 for a record that has not been saved yet.
 */
data class HealthRecord(
    val id: Long = 0,
    val date: LocalDate,
    val steps: Int,
    val calories: Int,
    val water: Int, // ml
) {
    val isSaved: Boolean get() = id != 0L
}

/** Steps, calories and water added up for one day (a day can have several records). */
data class DailyTotal(
    val date: LocalDate,
    val steps: Int = 0,
    val calories: Int = 0,
    val water: Int = 0,
) {
    val hasData: Boolean get() = steps > 0 || calories > 0 || water > 0
}
