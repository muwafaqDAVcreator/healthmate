package com.healthmate.app.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import com.healthmate.app.core.utils.AppDates
import java.time.LocalDate

/** Greys out future days in the date pickers, so a record can't be dated tomorrow. */
@OptIn(ExperimentalMaterial3Api::class)
object PastOrTodayDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        !AppDates.fromPickerMillis(utcTimeMillis).isAfter(LocalDate.now())

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}
