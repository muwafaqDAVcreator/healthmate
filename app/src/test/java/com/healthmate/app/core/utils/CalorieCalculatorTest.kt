package com.healthmate.app.core.utils

import com.healthmate.app.core.settings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun `stride length is 41_4 percent of height`() {
        assertEquals(0.7245, CalorieCalculator.strideLengthMeters(175), 0.0001)
    }

    @Test
    fun `distance is steps times stride length`() {
        // 10,000 steps x 0.7038 m = 7.04 km
        assertEquals(7.038, CalorieCalculator.distanceKm(10_000, 170), 0.001)
    }

    @Test
    fun `calories use weight, distance and 0_5 kcal per kg per km`() {
        // 175 cm, 70 kg, 7,850 steps -> 5.687 km -> 199 kcal
        assertEquals(199, CalorieCalculator.caloriesFromSteps(7850, 175, 70))
        // heavier person walking the same steps burns more
        assertEquals(256, CalorieCalculator.caloriesFromSteps(7850, 175, 90))
    }

    @Test
    fun `zero steps means zero calories`() {
        assertEquals(0, CalorieCalculator.caloriesFromSteps(0, 175, 70))
    }

    @Test
    fun `BMI is weight divided by height squared`() {
        assertEquals(22.86, UserSettings.bmiOf(175, 70)!!, 0.01)
        assertEquals("Healthy weight", UserSettings.bmiCategory(22.86))
        assertEquals("Overweight", UserSettings.bmiCategory(27.0))
        assertNull(UserSettings.bmiOf(null, 70))
    }
}
