package com.example.xq.flashcard.ui.practice

import com.example.xq.flashcard.ui.library.FlashCardItem
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

enum class ReviewDifficulty(val quality: Int) {
    HARD(2),
    MEDIUM(3),
    EASY(5)
}

data class Sm2ReviewResult(
    val easinessFactor: Double,
    val repetitions: Int,
    val intervalDays: Int,
    val nextReviewAt: Long
)

object Sm2Scheduler {

    const val DEFAULT_EASINESS_FACTOR = 2.5
    const val MIN_EASINESS_FACTOR = 1.3
    private const val FIRST_INTERVAL_DAYS = 1
    private const val SECOND_INTERVAL_DAYS = 6
    private const val MASTERED_INTERVAL_DAYS = 21

    fun applyReview(card: FlashCardItem, quality: Int, reviewedAt: Long): Sm2ReviewResult {
        val sanitizedQuality = quality.coerceIn(0, 5)
        val updatedFactor = max(
            MIN_EASINESS_FACTOR,
            card.easinessFactor + (0.1 - (5 - sanitizedQuality) * (0.08 + (5 - sanitizedQuality) * 0.02))
        )

        val repetitions = if (sanitizedQuality < 3) {
            0
        } else {
            card.repetitions + 1
        }

        val intervalDays = when {
            sanitizedQuality < 3 -> FIRST_INTERVAL_DAYS
            repetitions == 1 -> FIRST_INTERVAL_DAYS
            repetitions == 2 -> SECOND_INTERVAL_DAYS
            else -> ceil(card.intervalDays.coerceAtLeast(SECOND_INTERVAL_DAYS) * updatedFactor).toInt()
        }

        return Sm2ReviewResult(
            easinessFactor = updatedFactor,
            repetitions = repetitions,
            intervalDays = intervalDays,
            nextReviewAt = startOfDay(reviewedAt) + TimeUnit.DAYS.toMillis(intervalDays.toLong())
        )
    }

    fun isDueToday(card: FlashCardItem, now: Long): Boolean {
        return card.nextReviewAt == 0L || card.nextReviewAt <= endOfDay(now)
    }

    fun isDueNow(card: FlashCardItem, now: Long): Boolean {
        return card.nextReviewAt == 0L || card.nextReviewAt <= now
    }

    fun isLearning(card: FlashCardItem): Boolean {
        return card.repetitions < 2 || card.intervalDays <= SECOND_INTERVAL_DAYS
    }

    fun isMastered(card: FlashCardItem): Boolean {
        return card.repetitions >= 4 && card.intervalDays >= MASTERED_INTERVAL_DAYS
    }

    fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        return startOfDay(timestamp) + TimeUnit.DAYS.toMillis(1) - 1
    }

    fun dayOffset(timestamp: Long, now: Long): Int {
        val diff = startOfDay(timestamp) - startOfDay(now)
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }
}
