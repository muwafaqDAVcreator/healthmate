package com.healthmate.app

import android.app.Application
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.core.settings.SharedPrefsSettingsRepository
import com.healthmate.app.features.food.data.FoodRepository
import com.healthmate.app.features.food.data.SqliteFoodRepository
import com.healthmate.app.features.records.data.HealthRecordRepository
import com.healthmate.app.features.records.data.SqliteHealthRecordRepository

/**
 * Creates the database and repositories once for the whole app.
 * View models get them from here through [com.healthmate.app.core.ui.AppViewModelProvider].
 */
class HealthMateApplication : Application() {

    /** One SQLite database shared by the health records and food diary. */
    private val database by lazy { HealthDatabaseHelper(this) }

    val repository: HealthRecordRepository by lazy { SqliteHealthRecordRepository(database) }

    val foodRepository: FoodRepository by lazy { SqliteFoodRepository(database) }

    val settingsRepository: SettingsRepository by lazy { SharedPrefsSettingsRepository(this) }
}
