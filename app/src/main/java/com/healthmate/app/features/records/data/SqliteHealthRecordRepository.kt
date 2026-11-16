package com.healthmate.app.features.records.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_CALORIES
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_DATE
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_ID
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_STEPS
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.COL_WATER
import com.healthmate.app.core.database.HealthDatabaseHelper.Companion.TABLE_HEALTH_RECORDS
import com.healthmate.app.core.utils.AppDates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The only class that writes SQL. Every call runs on [ioDispatcher]
 * (a background thread) so the UI never freezes while SQLite works.
 */
class SqliteHealthRecordRepository(
    private val dbHelper: HealthDatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HealthRecordRepository {

    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()

    // ---------- Create ----------

    override suspend fun addRecord(record: HealthRecord): Long = write { db ->
        db.insertOrThrow(TABLE_HEALTH_RECORDS, null, record.toContentValues(includeId = false))
    }

    override suspend fun restoreRecord(record: HealthRecord): Long = write { db ->
        db.insertOrThrow(TABLE_HEALTH_RECORDS, null, record.toContentValues(includeId = true))
    }

    // ---------- Read ----------

    override suspend fun getRecords(from: LocalDate?, to: LocalDate?): List<HealthRecord> =
        withContext(ioDispatcher) {
            val (selection, args) = dateFilter(from, to)
            dbHelper.readableDatabase.query(
                TABLE_HEALTH_RECORDS,
                null,
                selection,
                args,
                null,
                null,
                "$COL_DATE DESC, $COL_ID DESC",
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.toHealthRecord()) }
            }
        }

    override suspend fun getRecordById(id: Long): HealthRecord? = withContext(ioDispatcher) {
        dbHelper.readableDatabase.query(
            TABLE_HEALTH_RECORDS, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null,
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toHealthRecord() else null
        }
    }

    override suspend fun getTotalsForDate(date: LocalDate): DailyTotal = withContext(ioDispatcher) {
        dbHelper.readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM($COL_STEPS), 0),
                   COALESCE(SUM($COL_CALORIES), 0),
                   COALESCE(SUM($COL_WATER), 0)
            FROM $TABLE_HEALTH_RECORDS
            WHERE $COL_DATE = ?
            """.trimIndent(),
            arrayOf(AppDates.toDb(date)),
        ).use { cursor ->
            cursor.moveToFirst()
            DailyTotal(date, cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))
        }
    }

    override suspend fun getDailyTotals(from: LocalDate, to: LocalDate): List<DailyTotal> =
        withContext(ioDispatcher) {
            dbHelper.readableDatabase.rawQuery(
                """
                SELECT $COL_DATE, SUM($COL_STEPS), SUM($COL_CALORIES), SUM($COL_WATER)
                FROM $TABLE_HEALTH_RECORDS
                WHERE $COL_DATE BETWEEN ? AND ?
                GROUP BY $COL_DATE
                ORDER BY $COL_DATE ASC
                """.trimIndent(),
                arrayOf(AppDates.toDb(from), AppDates.toDb(to)),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DailyTotal(
                                date = AppDates.fromDb(cursor.getString(0)),
                                steps = cursor.getInt(1),
                                calories = cursor.getInt(2),
                                water = cursor.getInt(3),
                            ),
                        )
                    }
                }
            }
        }

    // ---------- Update ----------

    override suspend fun updateRecord(record: HealthRecord): Int {
        require(record.isSaved) { "Cannot update a record that has not been saved" }
        return write { db ->
            db.update(
                TABLE_HEALTH_RECORDS,
                record.toContentValues(includeId = false),
                "$COL_ID = ?",
                arrayOf(record.id.toString()),
            )
        }
    }

    // ---------- Delete ----------

    override suspend fun deleteRecord(id: Long): Int = write { db ->
        db.delete(TABLE_HEALTH_RECORDS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ---------- Helpers ----------

    /** Runs a write on the background thread, then tells listeners the data changed. */
    private suspend fun <T> write(block: (SQLiteDatabase) -> T): T {
        val result = withContext(ioDispatcher) { block(dbHelper.writableDatabase) }
        _changes.update { it + 1 }
        return result
    }

    private fun dateFilter(from: LocalDate?, to: LocalDate?): Pair<String?, Array<String>?> = when {
        from != null && to != null ->
            "$COL_DATE BETWEEN ? AND ?" to arrayOf(AppDates.toDb(from), AppDates.toDb(to))
        from != null -> "$COL_DATE >= ?" to arrayOf(AppDates.toDb(from))
        to != null -> "$COL_DATE <= ?" to arrayOf(AppDates.toDb(to))
        else -> null to null
    }

    private fun HealthRecord.toContentValues(includeId: Boolean) = ContentValues().apply {
        if (includeId) put(COL_ID, id)
        put(COL_DATE, AppDates.toDb(date))
        put(COL_STEPS, steps)
        put(COL_CALORIES, calories)
        put(COL_WATER, water)
    }

    private fun Cursor.toHealthRecord() = HealthRecord(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        date = AppDates.fromDb(getString(getColumnIndexOrThrow(COL_DATE))),
        steps = getInt(getColumnIndexOrThrow(COL_STEPS)),
        calories = getInt(getColumnIndexOrThrow(COL_CALORIES)),
        water = getInt(getColumnIndexOrThrow(COL_WATER)),
    )
}
