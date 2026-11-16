package com.healthmate.app.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsValidatorsTest {

    @Test
    fun `TC-S1 height and weight are optional`() {
        assertNull(SettingsValidators.height(""))
        assertNull(SettingsValidators.weight(""))
    }

    @Test
    fun `TC-S2 unrealistic height or weight is rejected`() {
        assertEquals("Height should be between 100 and 250 cm", SettingsValidators.height("40"))
        assertEquals("Weight should be between 30 and 300 kg", SettingsValidators.weight("500"))
        assertNull(SettingsValidators.height("175"))
        assertNull(SettingsValidators.weight("70"))
    }

    @Test
    fun `TC-S3 goals are required and must be in range`() {
        assertEquals("Please enter a goal", SettingsValidators.stepsGoal(""))
        assertEquals("Steps goal should be between 1,000 and 100,000", SettingsValidators.stepsGoal("200"))
        assertEquals("Water goal should be between 500 and 10,000", SettingsValidators.waterGoal("20000"))
        assertNull(SettingsValidators.caloriesGoal("500"))
    }

    @Test
    fun `TC-S4 a very long name is rejected`() {
        assertEquals("Name is too long (max 30 characters)", SettingsValidators.name("A".repeat(31)))
        assertNull(SettingsValidators.name("Muwafaq"))
        assertNull(SettingsValidators.name(""))
    }
}
