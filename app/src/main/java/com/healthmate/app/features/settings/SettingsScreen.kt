package com.healthmate.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.constants.HealthMetric
import com.healthmate.app.core.settings.UserSettings
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.utils.CalorieCalculator
import com.healthmate.app.core.utils.formatted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileCard(state, viewModel)
            GoalsCard(state, viewModel)
            CalorieInfoCard(heightCm = state.heightCm, weightKg = state.weightKg)

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save settings")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard(title = "Profile") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val initial = state.name.trim().firstOrNull()?.uppercaseChar()
                if (initial != null) {
                    Text(
                        initial.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_name"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsNumberField(
                value = state.height,
                onValueChange = viewModel::onHeightChange,
                label = "Height",
                unit = "cm",
                icon = Icons.Rounded.Height,
                error = state.heightError,
                tag = "settings_height",
                modifier = Modifier.weight(1f),
            )
            SettingsNumberField(
                value = state.weight,
                onValueChange = viewModel::onWeightChange,
                label = "Weight",
                unit = "kg",
                icon = Icons.Rounded.MonitorWeight,
                error = state.weightError,
                tag = "settings_weight",
                modifier = Modifier.weight(1f),
            )
        }
        val bmi = state.bmi
        Text(
            if (bmi != null) {
                "BMI ${String.format(Locale.UK, "%.1f", bmi)} · ${UserSettings.bmiCategory(bmi)}"
            } else {
                "Add your height and weight to see your BMI and calculate calories."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalsCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard(title = "Daily goals") {
        SettingsNumberField(
            value = state.stepsGoal,
            onValueChange = viewModel::onStepsGoalChange,
            label = "Steps goal",
            unit = HealthMetric.STEPS.unit,
            icon = HealthMetric.STEPS.icon,
            iconTint = HealthMetric.STEPS,
            error = state.stepsGoalError,
            tag = "settings_goal_steps",
        )
        SettingsNumberField(
            value = state.caloriesGoal,
            onValueChange = viewModel::onCaloriesGoalChange,
            label = "Calories goal",
            unit = HealthMetric.CALORIES.unit,
            icon = HealthMetric.CALORIES.icon,
            iconTint = HealthMetric.CALORIES,
            error = state.caloriesGoalError,
            tag = "settings_goal_calories",
        )
        SettingsNumberField(
            value = state.waterGoal,
            onValueChange = viewModel::onWaterGoalChange,
            label = "Water goal",
            unit = HealthMetric.WATER.unit,
            icon = HealthMetric.WATER.icon,
            iconTint = HealthMetric.WATER,
            error = state.waterGoalError,
            tag = "settings_goal_water",
            imeAction = ImeAction.Done,
        )
    }
}

/** Explains the calorie formula, with a worked example using the user's own numbers. */
@Composable
private fun CalorieInfoCard(heightCm: Int?, weightKg: Int?) {
    SectionCard(title = "How calories are calculated", icon = Icons.Rounded.Calculate) {
        Text(
            "On the Add record screen, \"Calculate from steps\" estimates calories burned from walking:",
            style = MaterialTheme.typography.bodyMedium,
        )
        FormulaLine("1", "Stride length = height × 0.414")
        FormulaLine("2", "Distance = steps × stride length")
        FormulaLine("3", "Calories = weight (kg) × distance (km) × 0.5")
        HorizontalDivider()

        val exampleSteps = 10_000
        val exampleText = if (heightCm != null && weightKg != null && heightCm in 100..250 && weightKg in 30..300) {
            val stride = CalorieCalculator.strideLengthMeters(heightCm)
            val km = CalorieCalculator.distanceKm(exampleSteps, heightCm)
            val kcal = CalorieCalculator.caloriesFromSteps(exampleSteps, heightCm, weightKg)
            "For you: stride ${String.format(Locale.UK, "%.2f", stride)} m, so ${exampleSteps.formatted()} steps " +
                "≈ ${String.format(Locale.UK, "%.1f", km)} km ≈ ${kcal.formatted()} kcal."
        } else {
            "Example (170 cm, 70 kg): ${exampleSteps.formatted()} steps ≈ " +
                "${String.format(Locale.UK, "%.1f", CalorieCalculator.distanceKm(exampleSteps, 170))} km ≈ " +
                "${CalorieCalculator.caloriesFromSteps(exampleSteps, 170, 70).formatted()} kcal."
        }
        Text(exampleText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "This is an estimate. You can always type your own calories instead.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormulaLine(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun SettingsNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    icon: ImageVector,
    error: String?,
    tag: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    iconTint: HealthMetric? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = iconTint?.color ?: MaterialTheme.colorScheme.onSurfaceVariant)
        },
        suffix = { Text(unit) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier.testTag(tag),
    )
}
