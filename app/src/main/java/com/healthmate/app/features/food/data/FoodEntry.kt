package com.healthmate.app.features.food.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.LocalTime

enum class Meal(val label: String, val icon: ImageVector) {
    BREAKFAST("Breakfast", Icons.Rounded.BakeryDining),
    LUNCH("Lunch", Icons.Rounded.LunchDining),
    DINNER("Dinner", Icons.Rounded.DinnerDining),
    SNACK("Snack", Icons.Rounded.Fastfood);

    companion object {
        /** Sensible default meal for the time of day. */
        fun forTime(time: LocalTime): Meal = when {
            time.isBefore(LocalTime.of(10, 30)) -> BREAKFAST
            time.isBefore(LocalTime.of(15, 0)) -> LUNCH
            time.isBefore(LocalTime.of(18, 0)) -> SNACK
            else -> DINNER
        }
    }
}

/**
 * One row of the `food_entries` table: something the user ate.
 * [calories] is the total for all [servings] (already multiplied).
 */
data class FoodEntry(
    val id: Long = 0,
    val date: LocalDate,
    val meal: Meal,
    val name: String,
    val servings: Double,
    val calories: Int,
)
