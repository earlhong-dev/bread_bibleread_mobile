package com.bibleread.bread.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyVerseSchedulerTest {
    @Test
    fun `returns tomorrow when current time is past the target`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(timeZone).apply {
            set(2024, Calendar.JANUARY, 2, 12, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextTime = DailyVerseScheduler.getNextAlarmTime(
            now = calendar.timeInMillis,
            targetHour = 0,
            targetMinute = 0,
            timeZone = timeZone
        )

        val nextCalendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nextTime
        }

        assertEquals(2024, nextCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCalendar.get(Calendar.MONTH))
        assertEquals(3, nextCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, nextCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCalendar.get(Calendar.MINUTE))
    }

    @Test
    fun `returns today when current time is before the target`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(timeZone).apply {
            set(2024, Calendar.JANUARY, 2, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextTime = DailyVerseScheduler.getNextAlarmTime(
            now = calendar.timeInMillis,
            targetHour = 12,
            targetMinute = 0,
            timeZone = timeZone
        )

        val nextCalendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nextTime
        }

        assertEquals(2024, nextCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCalendar.get(Calendar.MONTH))
        assertEquals(2, nextCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, nextCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCalendar.get(Calendar.MINUTE))
    }
}
