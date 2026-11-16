package com.healthmate.app.features.records

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthmate.app.core.ui.AppViewModelProvider
import com.healthmate.app.core.ui.ErrorBanner
import com.healthmate.app.core.ui.PastOrTodayDates
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.features.records.data.HealthRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    snackbarHostState: SnackbarHostState,
    onRecordClick: (HealthRecord) -> Unit,
    viewModel: RecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRangePicker by rememberSaveable { mutableStateOf(false) }

    fun deleteWithUndo(record: HealthRecord) {
        viewModel.deleteRecord(record)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Record deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(record)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My records", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FilterBar(
                state = state,
                onFilterSelected = { filter ->
                    if (filter == RecordFilter.CUSTOM) showRangePicker = true else viewModel.setFilter(filter)
                },
            )

            state.errorMessage?.let { message ->
                ErrorBanner(
                    message = message,
                    onRetry = viewModel::loadRecords,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                state.isLoading && state.records.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.records.isEmpty() -> EmptyState(
                    isFiltered = state.isFiltered,
                    onClearFilter = viewModel::clearFilter,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        val count = state.records.size
                        Text(
                            "$count ${if (count == 1) "record" else "records"} · swipe left to delete",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.records, key = { it.id }) { record ->
                        SwipeToDeleteRecord(
                            record = record,
                            onDelete = ::deleteWithUndo,
                            onClick = { onRecordClick(record) },
                        )
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        DateRangeDialog(
            initial = state.customRange,
            onDismiss = { showRangePicker = false },
            onConfirm = { range ->
                showRangePicker = false
                viewModel.setFilter(RecordFilter.CUSTOM, range)
            },
        )
    }
}

@Composable
private fun FilterBar(state: RecordsUiState, onFilterSelected: (RecordFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(RecordFilter.entries) { filter ->
            val label = if (filter == RecordFilter.CUSTOM && state.filter == RecordFilter.CUSTOM) {
                state.customRange?.label ?: filter.label
            } else {
                filter.label
            }
            FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRecord(
    record: HealthRecord,
    onDelete: (HealthRecord) -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(record)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
    ) {
        RecordTile(record = record, onClick = onClick)
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean, onClearFilter: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (isFiltered) "No records for these dates" else "No records yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isFiltered) "Try another date range." else "Tap \"Add record\" to log your first day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (isFiltered) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onClearFilter) { Text("Show all records") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    initial: DateRange?,
    onDismiss: () -> Unit,
    onConfirm: (DateRange) -> Unit,
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.start?.let(AppDates::toPickerMillis),
        initialSelectedEndDateMillis = initial?.end?.let(AppDates::toPickerMillis),
        selectableDates = PastOrTodayDates,
    )
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis ?: start

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    if (start != null && end != null) {
                        onConfirm(DateRange(AppDates.fromPickerMillis(start), AppDates.fromPickerMillis(end)))
                    }
                },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            title = { Text("Show records between", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            showModeToggle = false,
        )
    }
}
