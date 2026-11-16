package com.healthmate.app.features.records.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.theme.HealthMateTheme
import com.healthmate.app.helpers.FakeSettingsRepository
import com.healthmate.app.helpers.createTestDatabase
import com.healthmate.app.helpers.createTestRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests: the real Add form drawn by Compose, tapped like a user would. */
@RunWith(RobolectricTestRunner::class)
class RecordFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var dbHelper: HealthDatabaseHelper

    @Before
    fun setUp() {
        dbHelper = createTestDatabase()
        val viewModel = RecordFormViewModel(createTestRepository(dbHelper), FakeSettingsRepository(), SavedStateHandle())
        composeRule.setContent {
            HealthMateTheme {
                RecordFormScreen(onBack = {}, onFinished = {}, viewModel = viewModel)
            }
        }
    }

    @After
    fun tearDown() = dbHelper.close()

    @Test
    fun saving_an_empty_form_shows_an_error_under_each_field() {
        composeRule.onNodeWithText("Save record").performScrollTo().performClick()

        composeRule.onNodeWithText("Please enter steps (use 0 if none)").assertIsDisplayed()
        composeRule.onNodeWithText("Please enter calories (use 0 if none)").assertIsDisplayed()
        composeRule.onNodeWithText("Please enter water intake (use 0 if none)").assertIsDisplayed()
    }

    @Test
    fun letters_typed_into_steps_are_ignored() {
        composeRule.onNodeWithTag("field_steps").performTextInput("12ab3")

        composeRule.onNodeWithText("123").assertIsDisplayed()
    }

    @Test
    fun all_zero_values_show_the_at_least_one_value_message() {
        composeRule.onNodeWithTag("field_steps").performTextInput("0")
        composeRule.onNodeWithTag("field_calories").performTextInput("0")
        composeRule.onNodeWithTag("field_water").performTextInput("0")
        composeRule.onNodeWithText("Save record").performScrollTo().performClick()

        composeRule.onNodeWithText("Enter at least one value above 0").assertIsDisplayed()
    }
}
