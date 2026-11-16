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

/**
 * Tells Compose how to build each view model. They all share the one
 * repository created in [HealthMateApplication].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { DashboardViewModel(healthMateApplication().repository) }
        initializer { RecordsViewModel(healthMateApplication().repository) }
        initializer {
            RecordFormViewModel(
                repository = healthMateApplication().repository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
    }
}

private fun CreationExtras.healthMateApplication(): HealthMateApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HealthMateApplication
