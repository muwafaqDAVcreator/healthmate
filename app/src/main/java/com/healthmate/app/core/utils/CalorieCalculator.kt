package com.healthmate.app.core.utils

import kotlin.math.roundToInt

/**
 * Estimates calories burned from walking, using the user's height and weight.
 *
 * 1. Stride length (m) = height (cm) × 0.414 ÷ 100
 * 2. Distance (km)     = steps × stride length ÷ 1000
 * 3. Calories (kcal)   = weight (kg) × distance (km) × 0.5
 *
 * 0.414 is a common pedometer estimate of stride as a share of height, and
 * walking burns roughly 0.5 kcal for every kg of body weight per km.
 * It is an estimate, so the user can still type their own value.
 */
object CalorieCalculator {
    const val STRIDE_FACTOR = 0.414
    const val KCAL_PER_KG_PER_KM = 0.5

    fun strideLengthMeters(heightCm: Int): Double = heightCm * STRIDE_FACTOR / 100

    fun distanceKm(steps: Int, heightCm: Int): Double = steps * strideLengthMeters(heightCm) / 1000

    fun caloriesFromSteps(steps: Int, heightCm: Int, weightKg: Int): Int =
        (weightKg * distanceKm(steps, heightCm) * KCAL_PER_KG_PER_KM).roundToInt()
}
