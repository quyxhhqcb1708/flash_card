package com.example.xq.flashcard.ui.practice

import android.content.Context
import com.example.xq.flashcard.R
import com.example.xq.flashcard.ui.library.FlashCardItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PracticeUiFormatter {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

    fun formatBucketTitle(context: Context, dayOffset: Int, now: Long): String {
        return when (dayOffset) {
            0 -> context.getString(R.string.practice_bucket_today)
            1 -> context.getString(R.string.practice_bucket_tomorrow)
            else -> {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = Sm2Scheduler.startOfDay(now)
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                dateFormat.format(Date(calendar.timeInMillis))
            }
        }
    }

    fun formatDueLabel(context: Context, card: FlashCardItem, now: Long): String {
        if (card.nextReviewAt == 0L) return context.getString(R.string.practice_due_new)
        val offset = Sm2Scheduler.dayOffset(card.nextReviewAt, now)
        return when {
            offset <= 0 -> context.getString(R.string.practice_due_today)
            offset == 1 -> context.getString(R.string.practice_due_tomorrow)
            else -> context.getString(R.string.practice_due_in_days, offset)
        }
    }

    fun formatSrsMeta(context: Context, card: FlashCardItem): String {
        return when {
            card.practiceCount == 0 -> context.getString(R.string.practice_meta_new)
            else -> context.getString(
                R.string.practice_meta_value,
                card.repetitions,
                card.intervalDays,
                card.easinessFactor
            )
        }
    }
}
