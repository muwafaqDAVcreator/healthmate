package com.healthmate.app.features.records.data

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Everything the view models need from the database.
 * Screens and view models only know about this interface, not about SQL.
 */
interface HealthRecordRepository {

    /**
     * Goes up by one after every insert, update or delete.
     * Both view models collect it and reload, so the dashboard and the
     * records list always match the database.
     */
    val changes: StateFlow<Int>

    /** Saves a new record and returns its id. */
    suspend fun addRecord(record: HealthRecord): Long

    /** Puts a deleted record back with its old id (used by Undo). */
    suspend fun restoreRecord(record: HealthRecord): Long

    /** Newest first. [from] and [to] are inclusive and both optional. */
    suspend fun getRecords(from: LocalDate? = null, to: LocalDate? = null): List<HealthRecord>

    suspend fun getRecordById(id: Long): HealthRecord?

    /** Sum of all records on [date]; zeros if there are none. */
    suspend fun getTotalsForDate(date: LocalDate): DailyTotal

    /** One total per day that has records, oldest first. */
    suspend fun getDailyTotals(from: LocalDate, to: LocalDate): List<DailyTotal>

    /** Returns the number of rows changed (1, or 0 if the id was not found). */
    suspend fun updateRecord(record: HealthRecord): Int

    /** Returns the number of rows deleted. */
    suspend fun deleteRecord(id: Long): Int
}
