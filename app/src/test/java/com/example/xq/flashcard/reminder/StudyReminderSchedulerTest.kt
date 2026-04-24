package com.example.xq.flashcard.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar

class StudyReminderSchedulerTest {

    @Test
    fun nextTriggerAt_returnsSameDayWhenTimeHasNotPassed() {
        val now = calendarOf(10, 15).timeInMillis

        val triggerAt = StudyReminderScheduler.nextTriggerAt(
            now = now,
            hourOfDay = 12,
            minute = 0
        )

        val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }
        val triggerCalendar = Calendar.getInstance().apply { timeInMillis = triggerAt }
        assertEquals(12, triggerCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCalendar.get(Calendar.MINUTE))
        assertEquals(
            nowCalendar.get(Calendar.DAY_OF_YEAR),
            triggerCalendar.get(Calendar.DAY_OF_YEAR)
        )
    }

    @Test
    fun nextTriggerAt_returnsNextDayWhenTimePassed() {
        val now = calendarOf(21, 30).timeInMillis

        val triggerAt = StudyReminderScheduler.nextTriggerAt(
            now = now,
            hourOfDay = 21,
            minute = 0
        )

        val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }
        val triggerCalendar = Calendar.getInstance().apply { timeInMillis = triggerAt }
        assertEquals(21, triggerCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCalendar.get(Calendar.MINUTE))
        assertNotEquals(
            nowCalendar.get(Calendar.DAY_OF_YEAR),
            triggerCalendar.get(Calendar.DAY_OF_YEAR)
        )
    }

    private fun calendarOf(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
