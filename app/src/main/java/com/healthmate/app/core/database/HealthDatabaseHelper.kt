package com.healthmate.app.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.healthmate.app.core.utils.AppDates
import java.time.LocalDate

/**
 * Opens the SQLite database and creates its tables.
 *
 * - `health_records` – the required table: steps, calories burned and water per entry.
 * - `food_entries`   – extra feature (added in version 2): food eaten and its calories.
 *
 * Android calls [onCreate] the first time the database file is opened, and
 * [onUpgrade] when an older version is already on the phone. The rest of
 * the app never uses this class directly: it goes through the repositories.
 *
 * @param databaseName pass null for an in-memory database (used in tests).
 * @param insertDummyData adds two weeks of sample records on first launch
 *   so the dashboard and chart have something to show straight away.
 */
class HealthDatabaseHelper(
    context: Context,
    databaseName: String? = DATABASE_NAME,
    private val insertDummyData: Boolean = true,
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
        // Most queries filter by date, so an index keeps them quick.
        db.execSQL(SQL_CREATE_DATE_INDEX)
        createFoodTable(db)

        if (insertDummyData) {
            insertDummyRecords(db)
            insertDummyFood(db)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 -> 2 adds the food diary. health_records is left untouched,
        // so users keep all their existing records.
        if (oldVersion < 2) {
            createFoodTable(db)
            if (insertDummyData) insertDummyFood(db)
        }
    }

    private fun createFoodTable(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_FOOD_TABLE)
        db.execSQL(SQL_CREATE_FOOD_DATE_INDEX)
    }

    /** Sample data counted back from today, so it never looks out of date. */
    private fun insertDummyRecords(db: SQLiteDatabase) {
        val today = LocalDate.now()
        db.beginTransaction()
        try {
            SAMPLE_DAYS.forEachIndexed { daysAgo, (steps, calories, water) ->
                db.insert(TABLE_HEALTH_RECORDS, null, rowOf(today.minusDays(daysAgo.toLong()), steps, calories, water))
            }
            // A second entry on one day shows that totals are added together.
            db.insert(TABLE_HEALTH_RECORDS, null, rowOf(today.minusDays(1), 2100, 130, 500))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** A few meals for today and yesterday so the food diary is not empty. */
    private fun insertDummyFood(db: SQLiteDatabase) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val meals = listOf(
            FoodSample(today, "BREAKFAST", "String hoppers", 1.0, 280),
            FoodSample(today, "BREAKFAST", "Milk tea with sugar", 1.0, 70),
            FoodSample(today, "LUNCH", "Rice and curry", 1.0, 650),
            FoodSample(today, "SNACK", "Banana", 1.0, 105),
            FoodSample(yesterday, "BREAKFAST", "Milk rice (kiribath)", 1.0, 330),
            FoodSample(yesterday, "LUNCH", "Kottu roti (chicken)", 1.0, 800),
            FoodSample(yesterday, "DINNER", "Pol roti", 2.0, 440),
        )
        db.beginTransaction()
        try {
            meals.forEach { meal ->
                db.insert(
                    TABLE_FOOD_ENTRIES,
                    null,
                    ContentValues().apply {
                        put(COL_DATE, AppDates.toDb(meal.date))
                        put(COL_MEAL, meal.meal)
                        put(COL_NAME, meal.name)
                        put(COL_SERVINGS, meal.servings)
                        put(COL_CALORIES, meal.calories)
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun rowOf(date: LocalDate, steps: Int, calories: Int, water: Int) = ContentValues().apply {
        put(COL_DATE, AppDates.toDb(date))
        put(COL_STEPS, steps)
        put(COL_CALORIES, calories)
        put(COL_WATER, water)
    }

    private data class FoodSample(
        val date: LocalDate,
        val meal: String,
        val name: String,
        val servings: Double,
        val calories: Int,
    )

    companion object {
        const val DATABASE_NAME = "healthmate.db"
        const val DATABASE_VERSION = 2

        // health_records (required by the brief)
        const val TABLE_HEALTH_RECORDS = "health_records"
        const val COL_ID = "id"
        const val COL_DATE = "date"
        const val COL_STEPS = "steps"
        const val COL_CALORIES = "calories"
        const val COL_WATER = "water"

        // food_entries (extra feature)
        const val TABLE_FOOD_ENTRIES = "food_entries"
        const val COL_MEAL = "meal"
        const val COL_NAME = "name"
        const val COL_SERVINGS = "servings"

        private const val SQL_CREATE_TABLE = """
            CREATE TABLE $TABLE_HEALTH_RECORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DATE TEXT NOT NULL,
                $COL_STEPS INTEGER NOT NULL DEFAULT 0,
                $COL_CALORIES INTEGER NOT NULL DEFAULT 0,
                $COL_WATER INTEGER NOT NULL DEFAULT 0
            )
        """

        private const val SQL_CREATE_DATE_INDEX =
            "CREATE INDEX idx_health_records_date ON $TABLE_HEALTH_RECORDS ($COL_DATE)"

        private const val SQL_CREATE_FOOD_TABLE = """
            CREATE TABLE $TABLE_FOOD_ENTRIES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DATE TEXT NOT NULL,
                $COL_MEAL TEXT NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_SERVINGS REAL NOT NULL DEFAULT 1,
                $COL_CALORIES INTEGER NOT NULL DEFAULT 0
            )
        """

        private const val SQL_CREATE_FOOD_DATE_INDEX =
            "CREATE INDEX idx_food_entries_date ON $TABLE_FOOD_ENTRIES ($COL_DATE)"

        /** steps, calories, water for each day, starting with today. */
        private val SAMPLE_DAYS = listOf(
            Triple(7850, 420, 2000),
            Triple(10230, 560, 2500),
            Triple(6400, 350, 1750),
            Triple(8920, 480, 2250),
            Triple(12050, 640, 3000),
            Triple(5300, 290, 1500),
            Triple(9100, 500, 2200),
            Triple(7600, 410, 1900),
            Triple(11200, 600, 2750),
            Triple(4800, 260, 1250),
            Triple(8300, 450, 2100),
            Triple(9750, 530, 2400),
            Triple(6900, 380, 1800),
            Triple(10500, 570, 2600),
        )
    }
}
