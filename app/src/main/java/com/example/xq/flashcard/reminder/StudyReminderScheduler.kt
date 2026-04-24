package com.example.xq.flashcard.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.xq.flashcard.ui.settings.AppSettingsStore
import java.util.Calendar
import java.util.concurrent.TimeUnit

object StudyReminderScheduler {

    private val reminderSlots = listOf(
        ReminderSlot(
            id = "noon",
            uniqueWorkName = "study_reminder_noon",
            hourOfDay = 12,
            minute = 0
        ),
        ReminderSlot(
            id = "evening",
            uniqueWorkName = "study_reminder_evening",
            hourOfDay = 21,
            minute = 0
        )
    )

    fun sync(context: Context) {
        if (AppSettingsStore.isNotificationEnabled(context)) {
            reminderSlots.forEach { scheduleSlot(context, it) }
        } else {
            cancelAll(context)
        }
    }

    internal fun scheduleNext(context: Context, slotId: String) {
        val slot = reminderSlots.firstOrNull { it.id == slotId } ?: return
        scheduleSlot(context, slot)
    }

    internal fun nextTriggerAt(now: Long, hourOfDay: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun scheduleSlot(context: Context, slot: ReminderSlot) {
        val now = System.currentTimeMillis()
        val delayMillis = (slot.nextTriggerAt(now) - now).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<StudyReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(StudyReminderWorker.KEY_SLOT_ID to slot.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            slot.uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        reminderSlots.forEach { slot ->
            workManager.cancelUniqueWork(slot.uniqueWorkName)
        }
    }

    private data class ReminderSlot(
        val id: String,
        val uniqueWorkName: String,
        val hourOfDay: Int,
        val minute: Int
    ) {
        fun nextTriggerAt(now: Long): Long {
            return StudyReminderScheduler.nextTriggerAt(now, hourOfDay, minute)
        }
    }
}
