package com.healthmate.app.features.food

import androidx.lifecycle.SavedStateHandle
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.settings.Goals
import com.healthmate.app.core.settings.UserSettings
import com.healthmate.app.features.food.add.AddFoodViewModel
import com.healthmate.app.features.food.data.FoodCalories
import com.healthmate.app.features.food.data.FoodCatalog
import com.healthmate.app.features.food.data.FoodEntry
import com.healthmate.app.features.food.data.Meal
import com.healthmate.app.features.food.data.SqliteFoodRepository
import com.healthmate.app.helpers.FakeSettingsRepository
import com.healthmate.app.helpers.MainDispatcherRule
import com.healthmate.app.helpers.createTestDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

/** Food diary: calorie maths, the food_entries table and both view models. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FoodTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 18)

    private lateinit var dbHelper: HealthDatabaseHelper
    private lateinit var foodRepository: SqliteFoodRepository

    @Before
    fun setUp() {
        dbHelper = createTestDatabase()
        foodRepository = SqliteFoodRepository(dbHelper, mainDispatcherRule.dispatcher)
    }

    @After
    fun tearDown() = dbHelper.close()

    private fun addFoodViewModel() = AddFoodViewModel(
        foodRepository = foodRepository,
        savedStateHandle = SavedStateHandle(mapOf(AddFoodViewModel.ARG_DATE to today.toEpochDay())),
        clock = { today },
        timeNow = { LocalTime.of(13, 0) },
    )

    private fun entry(calories: Int, meal: Meal = Meal.LUNCH, date: LocalDate = today) =
        FoodEntry(date = date, meal = meal, name = "Test food", servings = 1.0, calories = calories)

    // ---------- Calculation ----------

    @Test
    fun `TC-F1 calories eaten = calories per serving x servings`() {
        assertEquals(360, FoodCalories.total(240, 1.5)) // 1.5 cups of rice
        assertEquals(440, FoodCalories.total(220, 2.0)) // 2 pol roti
        assertEquals(53, FoodCalories.total(105, 0.5)) // half a banana, rounded
    }

    @Test
    fun `servings are shown without a needless decimal`() {
        assertEquals("1", FoodCalories.formatServings(1.0))
        assertEquals("1.5", FoodCalories.formatServings(1.5))
    }

    @Test
    fun `search finds foods ignoring case`() {
        val names = FoodCatalog.search("RICE").map { it.name }

        assertTrue("White rice" in names)
        assertTrue("Fried rice" in names)
        assertTrue(FoodCatalog.search("pizzaaa").isEmpty())
    }

    // ---------- food_entries table ----------

    @Test
    fun `TC-F2 food entries are saved and totalled per day`() = runTest {
        foodRepository.addEntry(entry(650))
        foodRepository.addEntry(entry(105, Meal.SNACK))
        foodRepository.addEntry(entry(999, date = today.minusDays(1)))

        assertEquals(2, foodRepository.getEntries(today).size)
        assertEquals(755, foodRepository.getTotalCalories(today))
        assertEquals(0, foodRepository.getTotalCalories(today.minusDays(5)))
    }

    @Test
    fun `TC-F3 a deleted food can be restored with Undo`() = runTest {
        val id = foodRepository.addEntry(entry(300))
        val saved = foodRepository.getEntries(today).single()

        foodRepository.deleteEntry(id)
        assertTrue(foodRepository.getEntries(today).isEmpty())

        foodRepository.restoreEntry(saved)
        assertEquals(listOf(saved), foodRepository.getEntries(today))
    }

    // ---------- Add food screen ----------

    @Test
    fun `TC-F4 picking a food and 2 servings saves the multiplied calories`() = runTest {
        val vm = addFoodViewModel()
        val rice = FoodCatalog.items.first { it.name == "White rice" }

        vm.selectFood(rice)
        vm.increaseServings() // 1.5
        vm.increaseServings() // 2
        assertEquals(480, vm.uiState.value.totalKcal)

        vm.save()
        advanceUntilIdle()

        val saved = foodRepository.getEntries(today).single()
        assertEquals("White rice", saved.name)
        assertEquals(2.0, saved.servings, 0.0)
        assertEquals(480, saved.calories)
        assertEquals(Meal.LUNCH, saved.meal) // 1 pm defaults to lunch
    }

    @Test
    fun `TC-F5 custom food needs a name and calories`() = runTest {
        val vm = addFoodViewModel()
        vm.openCustomFood()

        vm.save()

        assertEquals("Please enter the food name", vm.uiState.value.customNameError)
        assertEquals("Please enter calories per serving", vm.uiState.value.customKcalError)
        assertTrue(foodRepository.getEntries(today).isEmpty())
    }

    @Test
    fun `custom food is saved with the user's calories`() = runTest {
        val vm = addFoodViewModel()
        vm.openCustomFood()
        vm.onCustomNameChange("Chocolate bar")
        vm.onCustomKcalChange("230")

        vm.save()
        advanceUntilIdle()

        assertEquals(230, foodRepository.getEntries(today).single().calories)
        assertTrue(vm.uiState.value.savedMessage!!.contains("Chocolate bar"))
    }

    @Test
    fun `servings stay between 0_5 and 10`() {
        val vm = addFoodViewModel()
        vm.selectFood(FoodCatalog.items.first())

        repeat(5) { vm.decreaseServings() }
        assertEquals(0.5, vm.uiState.value.servings, 0.0)

        repeat(40) { vm.increaseServings() }
        assertEquals(10.0, vm.uiState.value.servings, 0.0)
    }

    // ---------- Food diary screen ----------

    @Test
    fun `food diary shows the day's total against the goal`() = runTest {
        foodRepository.addEntry(entry(650))
        foodRepository.addEntry(entry(350, Meal.DINNER))
        val settings = FakeSettingsRepository(UserSettings(goals = Goals(foodIntake = 1800)))
        val vm = FoodLogViewModel(foodRepository, settings, clock = { today })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1000, state.total)
        assertEquals(800, state.remaining)
        assertEquals(1, state.entriesFor(Meal.DINNER).size)
    }

    @Test
    fun `food diary can go back a day but not past today`() = runTest {
        foodRepository.addEntry(entry(500, date = today.minusDays(1)))
        val vm = FoodLogViewModel(foodRepository, FakeSettingsRepository(), clock = { today })
        advanceUntilIdle()

        vm.nextDay() // already today, nothing happens
        assertEquals(today, vm.uiState.value.date)

        vm.previousDay()
        advanceUntilIdle()
        assertEquals(500, vm.uiState.value.total)
        assertNull(vm.uiState.value.errorMessage)
    }
}
