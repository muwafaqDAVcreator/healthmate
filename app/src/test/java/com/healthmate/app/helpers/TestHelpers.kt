package com.healthmate.app.helpers

import androidx.test.core.app.ApplicationProvider
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.features.records.data.HealthRecord
import com.healthmate.app.features.records.data.SqliteHealthRecordRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate

/** A fresh in-memory database for each test, so tests never affect each other. */
fun createTestDatabase(withDummyData: Boolean = false) = HealthDatabaseHelper(
    context = ApplicationProvider.getApplicationContext(),
    databaseName = null,
    insertDummyData = withDummyData,
)

fun createTestRepository(
    dbHelper: HealthDatabaseHelper = createTestDatabase(),
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) = SqliteHealthRecordRepository(dbHelper, dispatcher)

fun record(date: LocalDate, steps: Int = 1000, calories: Int = 100, water: Int = 500) =
    HealthRecord(date = date, steps = steps, calories = calories, water = water)

/** Runs view model coroutines on a test dispatcher instead of the Android main thread. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
