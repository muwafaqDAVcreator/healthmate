package com.healthmate.app

import android.app.Application
import com.healthmate.app.core.database.HealthDatabaseHelper
import com.healthmate.app.core.settings.SettingsRepository
import com.healthmate.app.core.settings.SharedPrefsSettingsRepository
import com.healthmate.app.features.records.data.HealthRecordRepository
import com.healthmate.app.features.records.data.SqliteHealthRecordRepository

/**
 * Creates the database and repository once for the whole app.
 * View models get the repository from here through [AppViewModelProvider].
 */
class HealthMateApplication : Application() {

    val repository: HealthRecordRepository by lazy {
        SqliteHealthRecordRepository(HealthDatabaseHelper(this))
    }

    val settingsRepository: SettingsRepository by lazy {
        SharedPrefsSettingsRepository(this)
    }
}
