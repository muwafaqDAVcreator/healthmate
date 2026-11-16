package com.healthmate.app.core.utils

import java.text.NumberFormat
import java.util.Locale

private val wholeNumber = NumberFormat.getIntegerInstance(Locale.UK)

/** 7850 -> "7,850" */
fun Int.formatted(): String = wholeNumber.format(this)

/** Short label for chart bars: 8850 -> "8.9k", 420 -> "420". */
fun Int.compact(): String =
    if (this >= 1000) String.format(Locale.UK, "%.1fk", this / 1000.0) else toString()
