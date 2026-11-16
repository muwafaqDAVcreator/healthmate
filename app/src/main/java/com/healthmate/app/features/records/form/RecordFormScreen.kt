package com.healthmate.app.features.records.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.ui.PastOrTodayDates
import com.healthmate.app.core.utils.AppDates
import java.time.LocalDate

/**
 * Add / Edit form.
 * @param onFinished called with a message ("Record saved" etc.) when the form should close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(
    onBack: () -> Unit,
    onFinished: (String) -> Unit,
    viewModel: RecordFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.result) {
        state.result?.let { onFinished(it.message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit record" else "Add record") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete record")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateField(
                date = state.date,
                error = state.dateError,
                onClick = { showDatePicker = true },
            )
            NumberField(
                metric = HealthMetric.STEPS,
                value = state.steps,
                error = state.stepsError,
                onValueChange = viewModel::onStepsChange,
            )
            Column {
                NumberField(
                    metric = HealthMetric.CALORIES,
                    value = state.calories,
                    error = state.caloriesError,
                    helper = state.calorieHint,
                    onValueChange = viewModel::onCaloriesChange,
                )
                CalculateCaloriesRow(
                    enabled = state.canCalculateCalories,
                    onClick = viewModel::calculateCalories,
                )
            }
            NumberField(
                metric = HealthMetric.WATER,
                value = state.water,
                error = state.waterError,
                onValueChange = viewModel::onWaterChange,
                imeAction = ImeAction.Done,
            )

            state.formError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.isEditMode) "Update record" else "Save record")
                }
            }
        }
    }

    if (showDatePicker) {
        RecordDatePicker(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onDatePicked = { date ->
                showDatePicker = false
                viewModel.onDateChange(date)
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this record?") },
            text = { Text("This record will be removed from your history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Looks like a text field but opens the date picker when tapped. */
@Composable
private fun DateField(date: LocalDate, error: String?, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = AppDates.longDate(date),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .testTag("date_field")
                .clickable(onClickLabel = "Change date", onClick = onClick),
        )
    }
}

@Composable
private fun NumberField(
    metric: HealthMetric,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    helper: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(metric.label) },
        leadingIcon = { Icon(metric.icon, contentDescription = null, tint = metric.color) },
        suffix = { Text(metric.unit) },
        isError = error != null,
        supportingText = (error ?: helper)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_${metric.name.lowercase()}"),
    )
}

/**
 * "Calculate from steps" button under the calories field. When height and
 * weight are missing it explains where to add them instead.
 */
@Composable
private fun CalculateCaloriesRow(enabled: Boolean, onClick: () -> Unit) {
    if (enabled) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.testTag("calculate_calories"),
        ) {
            Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Calculate from steps")
        }
    } else {
        Text(
            "Tip: add your height and weight in Settings to calculate calories from steps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDatePicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = AppDates.toPickerMillis(initial),
        selectableDates = PastOrTodayDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = { pickerState.selectedDateMillis?.let { onDatePicked(AppDates.fromPickerMillis(it)) } },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}
