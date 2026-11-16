package com.healthmate.app.features.food

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.constants.FoodStyle
import com.healthmate.app.core.ui.AppCard
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.ui.ErrorBanner
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.core.utils.formatted
import com.healthmate.app.features.food.data.FoodCalories
import com.healthmate.app.features.food.data.FoodEntry
import com.healthmate.app.features.food.data.Meal
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogScreen(
    snackbarHostState: SnackbarHostState,
    onAddFood: (LocalDate) -> Unit,
    viewModel: FoodLogViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(entry: FoodEntry) {
        viewModel.delete(entry)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "${entry.name} removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(entry)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Food diary", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddFood(state.date) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add food") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DaySwitcher(
                    date = state.date,
                    isToday = state.isToday,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                )
            }
            state.errorMessage?.let { message ->
                item { ErrorBanner(message = message, onRetry = viewModel::load) }
            }
            item { DailyIntakeCard(state) }
            items(Meal.entries) { meal ->
                MealCard(
                    meal = meal,
                    entries = state.entriesFor(meal),
                    onDelete = ::deleteWithUndo,
                )
            }
        }
    }
}

@Composable
private fun DaySwitcher(date: LocalDate, isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Text(
            if (isToday) "Today" else AppDates.shortDate(date),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

/** Big total for the day with progress toward the food calorie goal. */
@Composable
private fun DailyIntakeCard(state: FoodLogUiState) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FoodStyle.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(FoodStyle.icon, contentDescription = null, tint = FoodStyle.color) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Calories eaten", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        Text(
                            state.total.formatted(),
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            " / ${state.goal.formatted()} kcal",
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = FoodStyle.color,
                trackColor = FoodStyle.color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.remaining >= 0) "${state.remaining.formatted()} kcal left today"
                else "${state.remaining.absoluteValue.formatted()} kcal over your goal",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MealCard(meal: Meal, entries: List<FoodEntry>, onDelete: (FoodEntry) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(meal.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    "${entries.sumOf { it.calories }.formatted()} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entries.isEmpty()) {
                Text(
                    "Nothing logged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 50.dp, top = 6.dp, bottom = 2.dp),
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 50.dp, end = 16.dp))
                    FoodRow(entry, onDelete = { onDelete(entry) })
                }
            }
        }
    }
}

@Composable
private fun FoodRow(entry: FoodEntry, onDelete: () -> Unit) {
    Row(
        Modifier.padding(start = 50.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${FoodCalories.formatServings(entry.servings)} serving${if (entry.servings == 1.0) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("${entry.calories.formatted()} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove ${entry.name}", tint = MaterialTheme.colorScheme.outline)
        }
    }
}
