package com.healthmate.app.features.settings

import androidx.test.core.app.ApplicationProvider
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.core.settings.SharedPrefsSettingsRepository
import com.healthmate.app.core.settings.UserSettings
import com.healthmate.app.helpers.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Uses the real SharedPreferences repository so saving is tested end to end. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newRepository() =
        SharedPrefsSettingsRepository(ApplicationProvider.getApplicationContext(), mainDispatcherRule.dispatcher)

    @Test
    fun `defaults are shown before anything is saved`() {
        val state = SettingsViewModel(newRepository()).uiState.value

        assertEquals("10000", state.stepsGoal)
        assertEquals("500", state.caloriesGoal)
        assertEquals("2500", state.waterGoal)
        assertEquals("", state.height)
    }

    @Test
    fun `TC-S5 saved settings are kept after the app restarts`() = runTest {
        val vm = SettingsViewModel(newRepository())
        vm.onNameChange("Muwafaq")
        vm.onHeightChange("175")
        vm.onWeightChange("70")
        vm.onStepsGoalChange("8000")

        vm.save()
        advanceUntilIdle()
        assertEquals("Settings saved", vm.uiState.value.message)

        // A new repository reads the same preferences file, like reopening the app.
        val reopened = newRepository().settings.value
        assertEquals(
            UserSettings(name = "Muwafaq", heightCm = 175, weightKg = 70, goals = Goals(steps = 8000)),
            reopened,
        )
    }

    @Test
    fun `invalid values show errors and are not saved`() = runTest {
        val repository = newRepository()
        val vm = SettingsViewModel(repository)
        vm.onHeightChange("50")
        vm.onWaterGoalChange("")

        vm.save()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.heightError)
        assertEquals("Please enter a goal", vm.uiState.value.waterGoalError)
        assertNull(vm.uiState.value.message)
        assertNull(repository.settings.value.heightCm)
    }

    @Test
    fun `BMI updates while typing`() {
        val vm = SettingsViewModel(newRepository())

        vm.onHeightChange("180")
        vm.onWeightChange("81")

        assertEquals(25.0, vm.uiState.value.bmi!!, 0.01)
    }
}
