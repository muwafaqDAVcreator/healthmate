package com.healthmate.app.features.records

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmate.app.core.utils.AppDates
import com.healthmate.app.features.records.data.HealthRecord
import com.healthmate.app.features.records.data.HealthRecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class RecordFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    CUSTOM("Pick dates"),
}

data class DateRange(val start: LocalDate, val end: LocalDate) {
    val label: String
        get() = if (start == end) AppDates.dayMonth(start)
        else "${AppDates.dayMonth(start)} – ${AppDates.dayMonth(end)}"
}

data class RecordsUiState(
    val records: List<HealthRecord> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val filter: RecordFilter = RecordFilter.ALL,
    val customRange: DateRange? = null,
) {
    val isFiltered: Boolean get() = filter != RecordFilter.ALL
}

/** State for the records list: the records, the active date filter and delete/undo. */
class RecordsViewModel(
    private val repository: HealthRecordRepository,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.changes.collect { loadRecords() }
        }
    }

    fun loadRecords() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (from, to) = dateRangeFor(_uiState.value)
                val records = repository.getRecords(from, to)
                _uiState.update { it.copy(records = records, isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Loading records failed", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Could not load your records.") }
            }
        }
    }

    fun setFilter(filter: RecordFilter, range: DateRange? = null) {
        _uiState.update {
            it.copy(
                filter = filter,
                customRange = if (filter == RecordFilter.CUSTOM) range else null,
            )
        }
        loadRecords()
    }

    fun clearFilter() = setFilter(RecordFilter.ALL)

    /**
     * Removes the record from the list straight away (so the swipe animation
     * finishes cleanly), then deletes it from the database.
     */
    fun deleteRecord(record: HealthRecord) {
        _uiState.update { state -> state.copy(records = state.records.filterNot { it.id == record.id }) }
        viewModelScope.launch {
            try {
                repository.deleteRecord(record.id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Delete failed", e)
                _uiState.update { it.copy(errorMessage = "Could not delete the record.") }
                loadRecords() // put it back on screen
            }
        }
    }

    /** Puts a deleted record back with the same id. */
    fun undoDelete(record: HealthRecord) {
        viewModelScope.launch {
            try {
                repository.restoreRecord(record)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Undo failed", e)
                _uiState.update { it.copy(errorMessage = "Could not restore the record.") }
            }
        }
    }

    private fun dateRangeFor(state: RecordsUiState): Pair<LocalDate?, LocalDate?> {
        val today = clock()
        return when (state.filter) {
            RecordFilter.ALL -> null to null
            RecordFilter.TODAY -> today to today
            RecordFilter.LAST_7_DAYS -> today.minusDays(6) to today
            RecordFilter.LAST_30_DAYS -> today.minusDays(29) to today
            RecordFilter.CUSTOM -> state.customRange?.start to state.customRange?.end
        }
    }

    private companion object {
        const val TAG = "RecordsViewModel"
    }
}
