package com.healthmate.app.core.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Reads and saves the user's profile and goals. */
interface SettingsRepository {
    /** Current settings; screens collect it so they update as soon as Settings is saved. */
    val settings: StateFlow<UserSettings>

    suspend fun save(settings: UserSettings)
}

/**
 * Keeps settings in Android SharedPreferences (a small key-value file).
 * They are kept apart from the SQLite database, which only stores health records.
 */
class SharedPrefsSettingsRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    override suspend fun save(settings: UserSettings) {
        withContext(ioDispatcher) {
            prefs.edit(commit = true) {
                putString(KEY_NAME, settings.name)
                putIntOrRemove(KEY_HEIGHT, settings.heightCm)
                putIntOrRemove(KEY_WEIGHT, settings.weightKg)
                putInt(KEY_GOAL_STEPS, settings.goals.steps)
                putInt(KEY_GOAL_CALORIES, settings.goals.calories)
                putInt(KEY_GOAL_WATER, settings.goals.water)
            }
        }
        _settings.value = settings
    }

    private fun read(): UserSettings {
        val defaults = Goals()
        return UserSettings(
            name = prefs.getString(KEY_NAME, "") ?: "",
            heightCm = prefs.getIntOrNull(KEY_HEIGHT),
            weightKg = prefs.getIntOrNull(KEY_WEIGHT),
            goals = Goals(
                steps = prefs.getInt(KEY_GOAL_STEPS, defaults.steps),
                calories = prefs.getInt(KEY_GOAL_CALORIES, defaults.calories),
                water = prefs.getInt(KEY_GOAL_WATER, defaults.water),
            ),
        )
    }

    private fun android.content.SharedPreferences.getIntOrNull(key: String): Int? =
        if (contains(key)) getInt(key, 0) else null

    private fun android.content.SharedPreferences.Editor.putIntOrRemove(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    private companion object {
        const val PREFS_NAME = "healthmate_settings"
        const val KEY_NAME = "name"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_GOAL_STEPS = "goal_steps"
        const val KEY_GOAL_CALORIES = "goal_calories"
        const val KEY_GOAL_WATER = "goal_water"
    }
}
