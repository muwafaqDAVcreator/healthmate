package com.healthmate.app.core.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.features.records.data.DailyTotal
import com.healthmate.app.features.records.data.HealthRecord

/**
 * The three things HealthMate tracks. Keeping the label, unit, icon and
 * colour together means every screen shows a metric the same way
 * (steps are always green, calories red and water blue).
 */
enum class HealthMetric(
    val label: String,
    val shortLabel: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color,
) {
    STEPS(
        label = "Steps",
        shortLabel = "Steps",
        unit = "steps",
        icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
        color = Color(0xFF43A047),
    ),
    CALORIES(
        label = "Calories burned",
        shortLabel = "Calories",
        unit = "kcal",
        icon = Icons.Rounded.LocalFireDepartment,
        color = Color(0xFFE53935),
    ),
    WATER(
        label = "Water intake",
        shortLabel = "Water",
        unit = "ml",
        icon = Icons.Rounded.WaterDrop,
        color = Color(0xFF1E88E5),
    );

    /** The user's goal for this metric (set on the Settings screen). */
    fun goalIn(goals: Goals): Int = when (this) {
        STEPS -> goals.steps
        CALORIES -> goals.calories
        WATER -> goals.water
    }

    fun valueIn(total: DailyTotal): Int = when (this) {
        STEPS -> total.steps
        CALORIES -> total.calories
        WATER -> total.water
    }

    fun valueIn(record: HealthRecord): Int = when (this) {
        STEPS -> record.steps
        CALORIES -> record.calories
        WATER -> record.water
    }
}
