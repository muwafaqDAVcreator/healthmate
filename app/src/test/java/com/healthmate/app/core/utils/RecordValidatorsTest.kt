package com.healthmate.app.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecordValidatorsTest {

    private val today = LocalDate.of(2026, 8, 18)

    @Test
    fun `TC-V1 empty steps asks the user to enter a value`() {
        assertEquals("Please enter steps (use 0 if none)", RecordValidators.steps(""))
        assertEquals("Please enter water intake (use 0 if none)", RecordValidators.water("   "))
    }

    @Test
    fun `TC-V2 letters and decimals are rejected`() {
        assertEquals("Use whole numbers only", RecordValidators.steps("12a"))
        assertEquals("Use whole numbers only", RecordValidators.calories("4.5"))
    }

    @Test
    fun `TC-V3 negative numbers are rejected`() {
        assertEquals("Cannot be negative", RecordValidators.water("-5"))
    }

    @Test
    fun `TC-V4 values above the limit are rejected`() {
        assertEquals("That looks too high (max 100,000)", RecordValidators.steps("100001"))
        assertEquals("That looks too high (max 10,000)", RecordValidators.water("10001"))
    }

    @Test
    fun `TC-V5 normal values and zero are accepted`() {
        assertNull(RecordValidators.steps("7850"))
        assertNull(RecordValidators.calories("0"))
        assertNull(RecordValidators.water("10000"))
    }

    @Test
    fun `TC-V6 a future date is rejected`() {
        assertEquals("Date cannot be in the future", RecordValidators.date(today.plusDays(1), today))
    }

    @Test
    fun `TC-V7 today and past dates are accepted`() {
        assertNull(RecordValidators.date(today, today))
        assertNull(RecordValidators.date(today.minusDays(30), today))
    }

    @Test
    fun `TC-V8 all values 0 is rejected`() {
        assertEquals("Enter at least one value above 0", RecordValidators.atLeastOneValue(0, 0, 0))
        assertNull(RecordValidators.atLeastOneValue(0, 0, 250))
    }
}
