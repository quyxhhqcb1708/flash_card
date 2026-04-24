package com.example.xq.flashcard.ui.progress

import com.example.xq.flashcard.ui.library.FlashCardItem
import com.example.xq.flashcard.ui.practice.Sm2Scheduler
import java.util.concurrent.TimeUnit

data class StudyStreakSummary(
    val currentStreakDays: Int,
    val studiedToday: Boolean
)

object StudyStreakCalculator {

    private val oneDayMillis = TimeUnit.DAYS.toMillis(1)

    fun build(
        cards: List<FlashCardItem>,
        now: Long = System.currentTimeMillis()
    ): StudyStreakSummary {
        val studiedDays = cards.asSequence()
            .map { it.lastPracticedAt }
            .filter { it > 0L }
            .map(Sm2Scheduler::startOfDay)
            .toSet()

        if (studiedDays.isEmpty()) {
            return StudyStreakSummary(
                currentStreakDays = 0,
                studiedToday = false
            )
        }

        val todayStart = Sm2Scheduler.startOfDay(now)
        val studiedToday = studiedDays.contains(todayStart)
        var cursor = if (studiedToday) todayStart else todayStart - oneDayMillis
        var streakDays = 0

        while (studiedDays.contains(cursor)) {
            streakDays += 1
            cursor -= oneDayMillis
        }

        return StudyStreakSummary(
            currentStreakDays = streakDays,
            studiedToday = studiedToday
        )
    }
}
