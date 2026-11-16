package com.healthmate.app.features.records.data

import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.helpers.createTestDatabase
import com.healthmate.app.helpers.createTestRepository
import com.healthmate.app.helpers.record
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Runs the real SQL against an in-memory SQLite database. */
@RunWith(RobolectricTestRunner::class)
class HealthRecordRepositoryTest {

    private lateinit var dbHelper: HealthDatabaseHelper
    private lateinit var repository: SqliteHealthRecordRepository

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 8, dayOfMonth)

    @Before
    fun setUp() {
        dbHelper = createTestDatabase()
        repository = createTestRepository(dbHelper)
    }

    @After
    fun tearDown() = dbHelper.close()

    @Test
    fun `TC-D1 Create - a new record is saved and gets an id`() = runTest {
        val id = repository.addRecord(record(day(18), steps = 7850))

        val saved = repository.getRecordById(id)

        assertTrue(id > 0)
        assertNotNull(saved)
        assertEquals(7850, saved!!.steps)
    }

    @Test
    fun `TC-D2 Read - records come back newest first`() = runTest {
        repository.addRecord(record(day(16)))
        repository.addRecord(record(day(18)))
        repository.addRecord(record(day(17)))

        val dates = repository.getRecords().map { it.date }

        assertEquals(listOf(day(18), day(17), day(16)), dates)
    }

    @Test
    fun `TC-D3 Update - changed values are saved`() = runTest {
        val id = repository.addRecord(record(day(18), water = 500))
        val original = repository.getRecordById(id)!!

        val rows = repository.updateRecord(original.copy(water = 2500))

        assertEquals(1, rows)
        assertEquals(2500, repository.getRecordById(id)!!.water)
    }

    @Test
    fun `TC-D4 Delete - the record is removed`() = runTest {
        val id = repository.addRecord(record(day(18)))

        val rows = repository.deleteRecord(id)

        assertEquals(1, rows)
        assertNull(repository.getRecordById(id))
    }

    @Test
    fun `TC-D5 Filter - only records inside the date range are returned`() = runTest {
        listOf(1, 10, 15, 20).forEach { repository.addRecord(record(day(it))) }

        val dates = repository.getRecords(from = day(10), to = day(15)).map { it.date }

        assertEquals(listOf(day(15), day(10)), dates)
    }

    @Test
    fun `TC-D6 Dashboard - two records on one day are added together`() = runTest {
        repository.addRecord(record(day(18), steps = 5000, calories = 300, water = 1500))
        repository.addRecord(record(day(18), steps = 2850, calories = 120, water = 500))
        repository.addRecord(record(day(17), steps = 9999))

        val total = repository.getTotalsForDate(day(18))

        assertEquals(7850, total.steps)
        assertEquals(420, total.calories)
        assertEquals(2000, total.water)
    }

    @Test
    fun `TC-D7 Dashboard - a day with no records shows zeros`() = runTest {
        val total = repository.getTotalsForDate(LocalDate.of(2026, 1, 1))

        assertEquals(0, total.steps)
        assertEquals(0, total.water)
    }

    @Test
    fun `TC-D8 Undo - a deleted record can be restored with the same id`() = runTest {
        val id = repository.addRecord(record(day(18), steps = 4321))
        val saved = repository.getRecordById(id)!!
        repository.deleteRecord(id)

        repository.restoreRecord(saved)

        assertEquals(saved, repository.getRecordById(id))
    }

    @Test
    fun `daily totals are grouped by day, oldest first`() = runTest {
        repository.addRecord(record(day(18), steps = 100))
        repository.addRecord(record(day(18), steps = 200))
        repository.addRecord(record(day(16), steps = 50))

        val totals = repository.getDailyTotals(day(15), day(18))

        assertEquals(listOf(day(16), day(18)), totals.map { it.date })
        assertEquals(300, totals.last().steps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updating an unsaved record is not allowed`() = runTest {
        repository.updateRecord(record(day(18)))
    }

    @Test
    fun `changes counter goes up after every write`() = runTest {
        val before = repository.changes.value

        val id = repository.addRecord(record(day(18)))
        repository.deleteRecord(id)

        assertEquals(before + 2, repository.changes.value)
    }

    @Test
    fun `dummy data is inserted on a fresh database`() = runTest {
        val seeded = createTestDatabase(withDummyData = true)
        val repo = createTestRepository(seeded)

        val all = repo.getRecords()
        val today = repo.getTotalsForDate(LocalDate.now())

        assertEquals(15, all.size)
        assertEquals(7850, today.steps)
        assertEquals(2000, today.water)
        seeded.close()
    }
}
