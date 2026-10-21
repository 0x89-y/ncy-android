package com.ncy.notechaty.ui.chat

import com.ncy.notechaty.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderSheetTest {

    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun presetAt(now: Long, label: Int): Long? =
        reminderPresets(now, zone).firstOrNull { it.label == label }?.at

    @Test
    fun `every preset lies in the future`() {
        val now = at(2026, 10, 21, 14, 30)
        assertTrue(reminderPresets(now, zone).all { it.at > now })
    }

    @Test
    fun `this evening is offered in the afternoon and dropped once evening is near`() {
        assertEquals(at(2026, 10, 21, 18), presetAt(at(2026, 10, 21, 14, 30), R.string.this_evening))
        assertFalse(reminderPresets(at(2026, 10, 21, 17, 15), zone).any { it.label == R.string.this_evening })
    }

    @Test
    fun `tomorrow morning is nine the next day, also across a month end`() {
        assertEquals(at(2026, 11, 1, 9), presetAt(at(2026, 10, 31, 22), R.string.tomorrow_morning))
    }

    @Test
    fun `next week is the coming Monday, a full week ahead on a Monday`() {
        // 21 October 2026 is a Wednesday.
        assertEquals(at(2026, 10, 26, 9), presetAt(at(2026, 10, 21, 10), R.string.next_week))
        assertEquals(at(2026, 11, 2, 9), presetAt(at(2026, 10, 26, 10), R.string.next_week))
    }

    @Test
    fun `a picked day is read as that calendar day, not shifted by the time zone`() {
        // The date picker hands over UTC midnight of the chosen day.
        val dayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.DECEMBER, 24)
        }.timeInMillis
        assertEquals(at(2026, 12, 24, 7, 45), combineDayAndTime(dayUtc, 7, 45, zone))
        val west = TimeZone.getTimeZone("America/Los_Angeles")
        val expected = Calendar.getInstance(west).apply { clear(); set(2026, Calendar.DECEMBER, 24, 7, 45) }.timeInMillis
        assertEquals(expected, combineDayAndTime(dayUtc, 7, 45, west))
    }
}
