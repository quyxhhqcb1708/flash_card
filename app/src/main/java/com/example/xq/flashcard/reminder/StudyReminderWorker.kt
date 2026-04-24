package com.example.xq.flashcard.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.xq.flashcard.R
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.main.MainActivity
import com.example.xq.flashcard.ui.practice.Sm2Scheduler
import com.example.xq.flashcard.ui.progress.StudyStreakCalculator
import com.example.xq.flashcard.ui.settings.AppSettingsStore

class StudyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val slotId = inputData.getString(KEY_SLOT_ID).orEmpty()
        if (slotId.isBlank()) {
            return Result.success()
        }

        return try {
            if (AppSettingsStore.isNotificationEnabled(applicationContext)) {
                showReminderIfNeeded()
                StudyReminderScheduler.scheduleNext(applicationContext, slotId)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showReminderIfNeeded() {
        val now = System.currentTimeMillis()
        val allEntries = FlashCardLibraryStore.getAllCards(applicationContext)
        if (allEntries.isEmpty()) return

        val streakSummary = StudyStreakCalculator.build(
            cards = allEntries.map { it.card },
            now = now
        )
        if (streakSummary.studiedToday) return

        val dueTodayCount = allEntries.count { Sm2Scheduler.isDueToday(it.card, now) }
        if (dueTodayCount <= 0) return

        ensureNotificationChannel()
        val openPracticeIntent = MainActivity.createPracticeIntent(applicationContext)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            openPracticeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = when {
            streakSummary.currentStreakDays > 0 -> {
                applicationContext.getString(
                    R.string.study_reminder_message_with_streak,
                    dueTodayCount,
                    streakSummary.currentStreakDays
                )
            }

            else -> {
                applicationContext.getString(
                    R.string.study_reminder_message_start_streak,
                    dueTodayCount
                )
            }
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_home_bell)
            .setContentTitle(applicationContext.getString(R.string.study_reminder_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.study_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(
                    R.string.study_reminder_channel_description
                )
            }
        )
    }

    companion object {
        const val KEY_SLOT_ID = "slot_id"
        private const val CHANNEL_ID = "study_reminders"
        private const val NOTIFICATION_ID = 12021
    }
}
