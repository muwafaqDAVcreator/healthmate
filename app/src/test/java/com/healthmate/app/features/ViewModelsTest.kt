package com.healthmate.app.features

import androidx.lifecycle.SavedStateHandle
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.features.dashboard.DashboardViewModel
import com.healthmate.app.features.records.DateRange
import com.healthmate.app.features.records.RecordFilter
import com.healthmate.app.features.records.RecordsViewModel
import com.healthmate.app.features.records.data.SqliteHealthRecordRepository
import com.healthmate.app.features.records.form.FormResult
import com.healthmate.app.features.records.form.RecordFormViewModel
import com.healthmate.app.helpers.MainDispatcherRule
import com.healthmate.app.helpers.createTestDatabase
import com.healthmate.app.helpers.createTestRepository
import com.healthmate.app.helpers.record
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

@RunWith(RobolectricTestRunner::class)
class ViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 18)
    private fun daysAgo(n: Long) = today.minusDays(n)

    private lateinit var dbHelper: HealthDatabaseHelper
    private lateinit var repository: SqliteHealthRecordRepository

    @Before
    fun setUp() {
        dbHelper = createTestDatabase()
        repository = createTestRepository(dbHelper, mainDispatcherRule.dispatcher)
    }

    @After
    fun tearDown() = dbHelper.close()

    private fun formViewModel(recordId: Long? = null) = RecordFormViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(
            recordId?.let { mapOf(RecordFormViewModel.ARG_RECORD_ID to it) } ?: emptyMap(),
        ),
        clock = { today },
    )

    // ---------- RecordsViewModel ----------

    @Test
    fun `Last 7 days filter hides older records`() = runTest {
        repository.addRecord(record(daysAgo(0), steps = 1))
        repository.addRecord(record(daysAgo(6), steps = 2))
        repository.addRecord(record(daysAgo(10), steps = 3))
        val vm = RecordsViewModel(repository, clock = { today })

        vm.setFilter(RecordFilter.LAST_7_DAYS)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), vm.uiState.value.records.map { it.steps })
        assertTrue(vm.uiState.value.isFiltered)
    }

    @Test
    fun `Custom range filter uses the picked dates`() = runTest {
        repository.addRecord(record(LocalDate.of(2026, 8, 10), steps = 1))
        repository.addRecord(record(LocalDate.of(2026, 8, 18), steps = 2))
        val vm = RecordsViewModel(repository, clock = { today })

        vm.setFilter(RecordFilter.CUSTOM, DateRange(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 18)))
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.records.single().steps)
    }

    @Test
    fun `deleting removes the record from the list and Undo brings it back`() = runTest {
        repository.addRecord(record(daysAgo(0), steps = 5))
        val vm = RecordsViewModel(repository, clock = { today })
        advanceUntilIdle()
        val saved = vm.uiState.value.records.single()

        vm.deleteRecord(saved)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.records.isEmpty())

        vm.undoDelete(saved)
        advanceUntilIdle()
        assertEquals(listOf(saved), vm.uiState.value.records)
    }

    // ---------- DashboardViewModel ----------

    @Test
    fun `dashboard shows today totals, 7 chart days and the streak`() = runTest {
        repository.addRecord(record(daysAgo(0), steps = 5000, calories = 250, water = 1250))
        repository.addRecord(record(daysAgo(1), steps = 8000, calories = 400, water = 2000))
        // Gap 2 days ago, so the streak should stop at 2.
        repository.addRecord(record(daysAgo(3), steps = 9000, calories = 450, water = 2200))
        val vm = DashboardViewModel(repository, clock = { today })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(5000, state.today.steps)
        assertEquals(7, state.lastSevenDays.size)
        assertEquals(today, state.lastSevenDays.last().date)
        assertEquals(2, state.streak)
        assertEquals(0.5f, state.progressFor(HealthMetric.STEPS)) // 5000 of 10000
        assertEquals(0.5f, state.progressFor(HealthMetric.WATER)) // 1250 of 2500
    }

    @Test
    fun `progress never goes above 100 percent`() = runTest {
        repository.addRecord(record(daysAgo(0), steps = 25000))
        val vm = DashboardViewModel(repository, clock = { today })
        advanceUntilIdle()

        assertEquals(1f, vm.uiState.value.progressFor(HealthMetric.STEPS))
    }

    @Test
    fun `dashboard updates by itself after a record is added`() = runTest {
        val vm = DashboardViewModel(repository, clock = { today })
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.today.water)

        repository.addRecord(record(daysAgo(0), water = 750))
        advanceUntilIdle()

        assertEquals(750, vm.uiState.value.today.water)
    }

    // ---------- RecordFormViewModel ----------

    @Test
    fun `TC-UI1 saving an empty form shows an error under each field`() = runTest {
        val vm = formViewModel()

        vm.save()

        val state = vm.uiState.value
        assertEquals("Please enter steps (use 0 if none)", state.stepsError)
        assertEquals("Please enter calories (use 0 if none)", state.caloriesError)
        assertEquals("Please enter water intake (use 0 if none)", state.waterError)
        assertNull(state.result)
    }

    @Test
    fun `TC-UI2 all values 0 shows the at least one value message`() = runTest {
        val vm = formViewModel()
        vm.onStepsChange("0")
        vm.onCaloriesChange("0")
        vm.onWaterChange("0")

        vm.save()

        assertEquals("Enter at least one value above 0", vm.uiState.value.formError)
    }

    @Test
    fun `TC-UI3 letters and signs are removed from number fields`() = runTest {
        val vm = formViewModel()

        vm.onStepsChange("12ab3")
        vm.onWaterChange("-500")

        assertEquals("123", vm.uiState.value.steps)
        assertEquals("500", vm.uiState.value.water)
    }

    @Test
    fun `a valid form is saved to the database`() = runTest {
        val vm = formViewModel()
        vm.onStepsChange("7850")
        vm.onCaloriesChange("420")
        vm.onWaterChange("2000")

        vm.save()
        advanceUntilIdle()

        assertEquals(FormResult.SAVED, vm.uiState.value.result)
        assertEquals(7850, repository.getRecords().single().steps)
    }

    @Test
    fun `edit mode loads the record and saves the change`() = runTest {
        val id = repository.addRecord(record(daysAgo(2), water = 500))
        val vm = formViewModel(recordId = id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEditMode)
        assertEquals("500", vm.uiState.value.water)

        vm.onWaterChange("2500")
        vm.save()
        advanceUntilIdle()

        assertEquals(FormResult.UPDATED, vm.uiState.value.result)
        assertEquals(2500, repository.getRecordById(id)!!.water)
    }

    @Test
    fun `delete from the edit screen removes the record`() = runTest {
        val id = repository.addRecord(record(daysAgo(0)))
        val vm = formViewModel(recordId = id)
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertEquals(FormResult.DELETED, vm.uiState.value.result)
        assertNull(repository.getRecordById(id))
    }
}
