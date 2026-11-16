package com.healthmate.app.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date helpers. Dates are saved in SQLite as `yyyy-MM-dd` text, which sorts
 * correctly as a string and makes "BETWEEN" filters simple.
 */
object AppDates {
    private val dbFormat = DateTimeFormatter.ISO_LOCAL_DATE
    private val longFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    private val shortFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.UK)
    private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val weekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.UK)
    private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.UK)

    fun toDb(date: LocalDate): String = date.format(dbFormat)

    fun fromDb(text: String): LocalDate = LocalDate.parse(text, dbFormat)

    /** 16 November 2026 */
    fun longDate(date: LocalDate): String = date.format(longFormat)

    /** Mon, 16 Nov 2026 */
    fun shortDate(date: LocalDate): String = date.format(shortFormat)

    /** 16 Nov */
    fun dayMonth(date: LocalDate): String = date.format(dayMonthFormat)

    /** Nov */
    fun monthShort(date: LocalDate): String = date.format(monthFormat).uppercase(Locale.UK)

    /** Mon */
    fun weekday(date: LocalDate): String = date.format(weekdayFormat)

    // Material date pickers work in UTC milliseconds.
    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
