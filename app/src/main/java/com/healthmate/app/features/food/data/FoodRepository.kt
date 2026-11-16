package com.healthmate.app.features.food.data

import android.content.ContentValues
import android.database.Cursor
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_CALORIES
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_DATE
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_ID
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_MEAL
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_NAME
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_SERVINGS
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.TABLE_FOOD_ENTRIES
import com.healthmate.app.core.utils.AppDates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Food diary data: add, read and delete what the user ate. */
interface FoodRepository {
    /** Goes up by one after every write, so screens know to reload. */
    val changes: StateFlow<Int>

    suspend fun addEntry(entry: FoodEntry): Long

    /** Puts a deleted entry back with its old id (used by Undo). */
    suspend fun restoreEntry(entry: FoodEntry): Long

    /** All entries on [date], in the order they were added. */
    suspend fun getEntries(date: LocalDate): List<FoodEntry>

    /** Total calories eaten on [date]; 0 if nothing was logged. */
    suspend fun getTotalCalories(date: LocalDate): Int

    suspend fun deleteEntry(id: Long): Int
}

/** Stores food entries in the `food_entries` SQLite table. */
class SqliteFoodRepository(
    private val dbHelper: HealthDatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FoodRepository {

    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()

    override suspend fun addEntry(entry: FoodEntry): Long = write {
        dbHelper.writableDatabase.insertOrThrow(TABLE_FOOD_ENTRIES, null, entry.toContentValues(includeId = false))
    }

    override suspend fun restoreEntry(entry: FoodEntry): Long = write {
        dbHelper.writableDatabase.insertOrThrow(TABLE_FOOD_ENTRIES, null, entry.toContentValues(includeId = true))
    }

    override suspend fun getEntries(date: LocalDate): List<FoodEntry> = withContext(ioDispatcher) {
        dbHelper.readableDatabase.query(
            TABLE_FOOD_ENTRIES, null, "$COL_DATE = ?", arrayOf(AppDates.toDb(date)), null, null, "$COL_ID ASC",
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toFoodEntry()) }
        }
    }

    override suspend fun getTotalCalories(date: LocalDate): Int = withContext(ioDispatcher) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM($COL_CALORIES), 0) FROM $TABLE_FOOD_ENTRIES WHERE $COL_DATE = ?",
            arrayOf(AppDates.toDb(date)),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    override suspend fun deleteEntry(id: Long): Int = write {
        dbHelper.writableDatabase.delete(TABLE_FOOD_ENTRIES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    private suspend fun <T> write(block: () -> T): T {
        val result = withContext(ioDispatcher) { block() }
        _changes.update { it + 1 }
        return result
    }

    private fun FoodEntry.toContentValues(includeId: Boolean) = ContentValues().apply {
        if (includeId) put(COL_ID, id)
        put(COL_DATE, AppDates.toDb(date))
        put(COL_MEAL, meal.name)
        put(COL_NAME, name)
        put(COL_SERVINGS, servings)
        put(COL_CALORIES, calories)
    }

    private fun Cursor.toFoodEntry() = FoodEntry(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        date = AppDates.fromDb(getString(getColumnIndexOrThrow(COL_DATE))),
        meal = runCatching { Meal.valueOf(getString(getColumnIndexOrThrow(COL_MEAL))) }.getOrDefault(Meal.SNACK),
        name = getString(getColumnIndexOrThrow(COL_NAME)),
        servings = getDouble(getColumnIndexOrThrow(COL_SERVINGS)),
        calories = getInt(getColumnIndexOrThrow(COL_CALORIES)),
    )
}
