package com.healthmate.app.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.healthmate.app.HealthMateApplication
import com.healthmate.app.features.dashboard.DashboardViewModel
import com.healthmate.app.features.records.RecordsViewModel
import com.healthmate.app.features.records.form.RecordFormViewModel
import com.healthmate.app.features.settings.SettingsViewModel

/**
 * Tells Compose how to build each view model. They all share the repositories
 * created once in [HealthMateApplication].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = healthMateApplication()
            DashboardViewModel(app.repository, app.settingsRepository)
        }
        initializer { RecordsViewModel(healthMateApplication().repository) }
        initializer {
            val app = healthMateApplication()
            RecordFormViewModel(
                repository = app.repository,
                settingsRepository = app.settingsRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer { SettingsViewModel(healthMateApplication().settingsRepository) }
    }
}

private fun CreationExtras.healthMateApplication(): HealthMateApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HealthMateApplication
