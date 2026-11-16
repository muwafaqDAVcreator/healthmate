package com.healthmate.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Checks that updating the app from database version 1 (no food diary)
 * to version 2 keeps every existing health record.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseUpgradeTest {

    @Test
    fun `TC-D9 upgrading to version 2 keeps health records and adds food_entries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("upgrade_test.db").apply { parentFile?.mkdirs(); delete() }

        // Build a version 1 database by hand, like an older install of the app.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                "CREATE TABLE health_records (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, " +
                    "steps INTEGER NOT NULL DEFAULT 0, calories INTEGER NOT NULL DEFAULT 0, water INTEGER NOT NULL DEFAULT 0)",
            )
            old.execSQL("INSERT INTO health_records (date, steps, calories, water) VALUES ('2026-08-18', 7850, 420, 2000)")
            old.version = 1
        }

        val helper = HealthDatabaseHelper(context, "upgrade_test.db", insertDummyData = false)
        val db = helper.readableDatabase

        assertEquals(2, db.version)
        db.rawQuery("SELECT steps FROM health_records", null).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(7850, cursor.getInt(0))
        }
        db.rawQuery("SELECT COUNT(*) FROM food_entries", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        helper.close()
    }
}
